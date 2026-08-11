package com.schoolerp.modules.settings.service;

import com.schoolerp.modules.fee.service.FeeCollectionService;
import com.schoolerp.modules.fee.dto.FeeDuesResponse;
import com.schoolerp.modules.settings.dto.PromotionDataResponse;
import com.schoolerp.modules.settings.dto.PromotionRequest;
import com.schoolerp.modules.settings.dto.StudentPromotionDto;
import com.schoolerp.modules.settings.dto.PromotionDecision;
import com.schoolerp.modules.student.dto.StudentSummaryDto;
import com.schoolerp.modules.student.entity.Student;
import com.schoolerp.modules.student.entity.StudentEnrollment;
import com.schoolerp.modules.student.repository.StudentEnrollmentRepository;
import com.schoolerp.modules.student.repository.StudentRepository;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicYearPromotionService {

    private final JdbcTemplate jdbcTemplate;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final FeeCollectionService feeCollectionService;

    public PromotionDataResponse getPromotionData() {
        UUID schoolId = TenantContext.getSchoolId();
        
        List<Map<String, Object>> activeYears = jdbcTemplate.queryForList(
                "SELECT id FROM academic_years WHERE school_id = ? AND is_current = true", schoolId);
        if (activeYears.isEmpty()) throw new IllegalStateException("No active academic year");
        UUID currentAcademicYearId = (UUID) activeYears.get(0).get("id");

        List<StudentSummaryDto> students = jdbcTemplate.query(
                "SELECT s.id, s.admission_number, s.first_name, s.last_name, s.status, " +
                "c.name as class_name, sec.name as section_name, s.father_name " +
                "FROM students s " +
                "JOIN student_enrollments e ON s.id = e.student_id " +
                "JOIN classes c ON e.class_id = c.id " +
                "JOIN sections sec ON e.section_id = sec.id " +
                "WHERE s.school_id = ? AND e.academic_year_id = ? AND e.status = 'ACTIVE' " +
                "ORDER BY c.numeric_order, sec.name, s.first_name",
                (rs, rowNum) -> {
                    StudentSummaryDto dto = new StudentSummaryDto();
                    dto.setId((UUID) rs.getObject("id"));
                    dto.setAdmissionNumber(rs.getString("admission_number"));
                    String fullName = rs.getString("first_name");
                    if (rs.getString("last_name") != null) {
                        fullName += " " + rs.getString("last_name");
                    }
                    dto.setFirstName(rs.getString("first_name"));
                    dto.setFullName(fullName);
                    dto.setFatherName(rs.getString("father_name"));
                    dto.setClassName(rs.getString("class_name"));
                    dto.setSectionName(rs.getString("section_name"));
                    dto.setStatus(rs.getString("status"));
                    return dto;
                },
                schoolId, currentAcademicYearId
        );

        // Fetch academic year details to calculate months for pending dues
        Map<String, Object> currentYear = jdbcTemplate.queryForMap(
                "SELECT start_date, end_date FROM academic_years WHERE id = ?", currentAcademicYearId);
        LocalDate startDate = ((java.sql.Date) currentYear.get("start_date")).toLocalDate();
        LocalDate endDate = ((java.sql.Date) currentYear.get("end_date")).toLocalDate();
        
        List<YearMonth> monthsToCheck = new java.util.ArrayList<>();
        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);
        while (!currentMonth.isAfter(endMonth)) {
            monthsToCheck.add(currentMonth);
            currentMonth = currentMonth.plusMonths(1);
        }

        for (StudentSummaryDto student : students) {
            BigDecimal pendingDues = BigDecimal.ZERO;
            for (YearMonth ym : monthsToCheck) {
                try {
                    com.schoolerp.modules.fee.dto.FeeDuesResponse dues = feeCollectionService.getDues(student.getId(), ym.getMonthValue(), ym.getYear());
                    if (!dues.isAlreadyPaid() && dues.getSubtotal() != null) {
                        pendingDues = pendingDues.add(dues.getSubtotal());
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
            student.setPendingDues(pendingDues);
        }

        List<Map<String, Object>> classRows = jdbcTemplate.queryForList(
                "SELECT c.id as class_id, c.name as class_name, c.numeric_order, s.id as section_id, s.name as section_name " +
                "FROM classes c " +
                "JOIN sections s ON c.id = s.class_id " +
                "WHERE c.school_id = ? AND c.is_active = true AND s.is_active = true " +
                "ORDER BY c.numeric_order, s.name", schoolId);

        Map<UUID, PromotionDataResponse.ClassData> classMap = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : classRows) {
            UUID cId = (UUID) row.get("class_id");
            PromotionDataResponse.ClassData cData = classMap.computeIfAbsent(cId, k -> 
                PromotionDataResponse.ClassData.builder()
                    .id(cId)
                    .name((String) row.get("class_name"))
                    .numericOrder((Integer) row.get("numeric_order"))
                    .sections(new ArrayList<>())
                    .build()
            );
            
            cData.getSections().add(PromotionDataResponse.SectionData.builder()
                    .id((UUID) row.get("section_id"))
                    .name((String) row.get("section_name"))
                    .build());
        }

        return PromotionDataResponse.builder()
                .currentStudents(students)
                .classes(new ArrayList<>(classMap.values()))
                .build();
    }

    @Transactional
    public void promoteStudents(PromotionRequest request) {
        UUID schoolId = TenantContext.getSchoolId();

        var currentYearRow = jdbcTemplate.queryForMap(
                "SELECT id, start_date, end_date FROM academic_years WHERE school_id = ? AND is_current = true", schoolId);
        UUID currentYearId = (UUID) currentYearRow.get("id");
        LocalDate currentStartDate = ((java.sql.Date) currentYearRow.get("start_date")).toLocalDate();
        LocalDate currentEndDate = ((java.sql.Date) currentYearRow.get("end_date")).toLocalDate();

        // Calculate arrears for all students before changing the academic year
        Map<UUID, BigDecimal> studentArrears = new java.util.HashMap<>();
        
        List<YearMonth> monthsToCheck = new ArrayList<>();
        YearMonth start = YearMonth.from(currentStartDate);
        YearMonth end = YearMonth.from(currentEndDate);
        YearMonth current = start;
        while (!current.isAfter(end)) {
            monthsToCheck.add(current);
            current = current.plusMonths(1);
        }

        for (StudentPromotionDto p : request.getPromotions()) {
            if (p.getDecision() == PromotionDecision.LEFT) {
                continue;
            }
            BigDecimal totalArrears = BigDecimal.ZERO;
            for (YearMonth ym : monthsToCheck) {
                try {
                    FeeDuesResponse dues = feeCollectionService.getDues(p.getStudentId(), ym.getMonthValue(), ym.getYear());
                    if (!dues.isAlreadyPaid() && dues.getSubtotal() != null) {
                        totalArrears = totalArrears.add(dues.getSubtotal());
                    }
                } catch (Exception e) {
                    // Could throw if enrollment not active, but we only calculate for currently enrolled students
                }
            }
            studentArrears.put(p.getStudentId(), totalArrears);
        }

        // Close current year
        jdbcTemplate.update("UPDATE academic_years SET is_current = false WHERE id = ?", currentYearId);

        // Insert new year
        UUID newYearId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO academic_years (id, school_id, name, start_date, end_date, is_current) VALUES (?, ?, ?, ?, ?, true)",
                newYearId, schoolId, request.getNewAcademicYearName(), request.getNewStartDate(), request.getNewEndDate()
        );

        // Copy fee structures to the new year
        jdbcTemplate.update(
                "INSERT INTO monthly_fee_structures (id, school_id, academic_year_id, class_id, fee_category_id, default_amount, created_at, updated_at) " +
                "SELECT gen_random_uuid(), school_id, ?, class_id, fee_category_id, default_amount, NOW(), NOW() " +
                "FROM monthly_fee_structures WHERE school_id = ? AND academic_year_id = ?",
                newYearId, schoolId, currentYearId
        );

        // Process promotions
        for (StudentPromotionDto p : request.getPromotions()) {
            Student student = studentRepository.findById(p.getStudentId()).orElse(null);
            if (student == null) continue;
            
            StudentEnrollment oldEnrollment = studentEnrollmentRepository
                    .findByStudentIdAndAcademicYearIdAndSchoolId(p.getStudentId(), currentYearId, schoolId)
                    .orElse(null);
            
            if (p.getDecision() == PromotionDecision.LEFT || p.getDecision() == PromotionDecision.GRADUATED) {
                student.setStatus(p.getDecision().name());
                studentRepository.save(student);
                if (oldEnrollment != null) {
                    oldEnrollment.setStatus(p.getDecision().name());
                    studentEnrollmentRepository.save(oldEnrollment);
                }
                continue;
            }
            
            if (oldEnrollment != null) {
                oldEnrollment.setStatus("COMPLETED");
                studentEnrollmentRepository.save(oldEnrollment);
            }

            BigDecimal arrears = studentArrears.getOrDefault(p.getStudentId(), BigDecimal.ZERO);
            if (oldEnrollment != null && oldEnrollment.getPreviousDues() != null) {
                arrears = arrears.add(oldEnrollment.getPreviousDues()); // Any unresolved arrears from even older years!
            }

            StudentEnrollment newEnrollment = StudentEnrollment.builder()
                    .schoolId(schoolId)
                    .studentId(p.getStudentId())
                    .academicYearId(newYearId)
                    .classId(p.getNextClassId())
                    .sectionId(p.getNextSectionId())
                    .enrollmentDate(request.getNewStartDate())
                    .status("ACTIVE")
                    .rollNumber(oldEnrollment != null ? oldEnrollment.getRollNumber() : null)
                    .previousDues(arrears)
                    .build();
            
            studentEnrollmentRepository.save(newEnrollment);
        }
    }
}
