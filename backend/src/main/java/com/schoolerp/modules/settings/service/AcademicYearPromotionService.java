package com.schoolerp.modules.settings.service;

import com.schoolerp.modules.academic.entity.SchoolClass;
import com.schoolerp.modules.academic.entity.Section;
import com.schoolerp.modules.academic.repository.ClassRepository;
import com.schoolerp.modules.academic.repository.SectionRepository;
import com.schoolerp.modules.fee.repository.PaymentTransactionRepository;
import com.schoolerp.modules.fee.service.FeeStructureLookup;
import com.schoolerp.modules.settings.dto.PromotionDataResponse;
import com.schoolerp.modules.settings.dto.PromotionDecision;
import com.schoolerp.modules.settings.dto.PromotionRequest;
import com.schoolerp.modules.settings.dto.StudentPromotionDto;
import com.schoolerp.modules.student.dto.StudentSummaryDto;
import com.schoolerp.modules.student.entity.Student;
import com.schoolerp.modules.student.entity.StudentEnrollment;
import com.schoolerp.modules.student.repository.StudentEnrollmentRepository;
import com.schoolerp.modules.student.repository.StudentRepository;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.security.TenantContext;
import com.schoolerp.shared.time.SchoolClock;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AcademicYearPromotionService {

    private final JdbcTemplate jdbcTemplate;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final FeeStructureLookup feeStructureLookup;
    private final SchoolClock clock;

    private record CurrentYear(UUID id, LocalDate startDate, LocalDate endDate) {}

    public PromotionDataResponse getPromotionData() {
        UUID schoolId = TenantContext.getSchoolId();
        CurrentYear year = currentYear(schoolId);

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
                schoolId, year.id());

        for (StudentSummaryDto dto : students) {
            Student student = studentRepository.findById(dto.getId()).orElse(null);
            StudentEnrollment enrollment = studentEnrollmentRepository
                    .findByStudentIdAndAcademicYearIdAndSchoolId(dto.getId(), year.id(), schoolId).orElse(null);
            dto.setPendingDues(student == null || enrollment == null
                    ? BigDecimal.ZERO
                    : calculateArrears(schoolId, student, enrollment, year));
        }

        List<Map<String, Object>> classRows = jdbcTemplate.queryForList(
                "SELECT c.id as class_id, c.name as class_name, c.numeric_order, s.id as section_id, s.name as section_name " +
                "FROM classes c " +
                "JOIN sections s ON c.id = s.class_id " +
                "WHERE c.school_id = ? AND c.is_active = true AND s.is_active = true " +
                "ORDER BY c.numeric_order, s.name", schoolId);

        Map<UUID, PromotionDataResponse.ClassData> classMap = new LinkedHashMap<>();
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
        CurrentYear year = currentYear(schoolId);
        validateNewYear(request, schoolId, year);

        // SEC-01: resolve and validate everything against this school before changing anything
        List<StudentPromotionDto> promotions = request.getPromotions() == null ? List.of() : request.getPromotions();
        Set<UUID> seen = new HashSet<>();
        Map<UUID, Student> students = new HashMap<>();
        Map<UUID, StudentEnrollment> enrollments = new HashMap<>();
        for (StudentPromotionDto p : promotions) {
            if (p.getStudentId() == null || p.getDecision() == null) {
                throw new BusinessException("Each student needs a promotion decision.");
            }
            if (!seen.add(p.getStudentId())) {
                throw new BusinessException("A student appears more than once in the promotion list.");
            }
            Student student = studentRepository.findById(p.getStudentId())
                    .filter(s -> s.getSchoolId().equals(schoolId))
                    .orElseThrow(() -> new BusinessException("One or more students were not found in this school."));
            StudentEnrollment enrollment = studentEnrollmentRepository
                    .findByStudentIdAndAcademicYearIdAndSchoolId(student.getId(), year.id(), schoolId)
                    .filter(e -> "ACTIVE".equals(e.getStatus()))
                    .orElseThrow(() -> new BusinessException(student.getFirstName()
                            + " is not actively enrolled in the current academic year."));
            if (p.getDecision() == PromotionDecision.PROMOTED || p.getDecision() == PromotionDecision.RETAINED) {
                validateTarget(p, schoolId, student);
            }
            students.put(student.getId(), student);
            enrollments.put(student.getId(), enrollment);
        }

        // Arrears are calculated before the current year is closed
        Map<UUID, BigDecimal> arrears = new HashMap<>();
        for (StudentPromotionDto p : promotions) {
            if (p.getDecision() == PromotionDecision.PROMOTED || p.getDecision() == PromotionDecision.RETAINED) {
                arrears.put(p.getStudentId(), calculateArrears(
                        schoolId, students.get(p.getStudentId()), enrollments.get(p.getStudentId()), year));
            }
        }

        // Close current year, open the new one, carry fee structures forward
        jdbcTemplate.update("UPDATE academic_years SET is_current = false WHERE id = ?", year.id());
        UUID newYearId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO academic_years (id, school_id, name, start_date, end_date, is_current) VALUES (?, ?, ?, ?, ?, true)",
                newYearId, schoolId, request.getNewAcademicYearName().trim(), request.getNewStartDate(), request.getNewEndDate());
        jdbcTemplate.update(
                "INSERT INTO monthly_fee_structures (id, school_id, academic_year_id, class_id, fee_category_id, default_amount, created_at, updated_at) " +
                "SELECT gen_random_uuid(), school_id, ?, class_id, fee_category_id, default_amount, NOW(), NOW() " +
                "FROM monthly_fee_structures WHERE school_id = ? AND academic_year_id = ?",
                newYearId, schoolId, year.id());

        for (StudentPromotionDto p : promotions) {
            Student student = students.get(p.getStudentId());
            StudentEnrollment oldEnrollment = enrollments.get(p.getStudentId());

            if (p.getDecision() == PromotionDecision.LEFT || p.getDecision() == PromotionDecision.GRADUATED) {
                student.setStatus(p.getDecision().name());
                studentRepository.save(student);
                oldEnrollment.setStatus(p.getDecision().name());
                studentEnrollmentRepository.save(oldEnrollment);
                continue;
            }

            oldEnrollment.setStatus("COMPLETED");
            studentEnrollmentRepository.save(oldEnrollment);

            studentEnrollmentRepository.save(StudentEnrollment.builder()
                    .schoolId(schoolId)
                    .studentId(student.getId())
                    .academicYearId(newYearId)
                    .classId(p.getNextClassId())
                    .sectionId(p.getNextSectionId())
                    .enrollmentDate(request.getNewStartDate())
                    .status("ACTIVE")
                    .rollNumber(oldEnrollment.getRollNumber())
                    .previousDues(arrears.getOrDefault(student.getId(), BigDecimal.ZERO))
                    .build());
        }
    }

    // ── Arrears ──────────────────────────────────────────────────────────────

    /**
     * Unpaid fees to carry into the next year (BUG-02, BUG-06):
     * each unpaid month counted once, only from the month the student joined up
     * to the current month, net of the student's concession, plus any older
     * arrears still outstanding (counted once).
     */
    BigDecimal calculateArrears(UUID schoolId, Student student, StudentEnrollment enrollment, CurrentYear year) {
        YearMonth from = YearMonth.from(year.startDate());
        if (enrollment.getEnrollmentDate() != null && YearMonth.from(enrollment.getEnrollmentDate()).isAfter(from)) {
            from = YearMonth.from(enrollment.getEnrollmentDate());
        }
        YearMonth to = YearMonth.from(year.endDate());
        if (clock.currentMonth().isBefore(to)) {
            to = clock.currentMonth();
        }

        BigDecimal monthlyFee = feeStructureLookup
                .monthlyTotal(schoolId, enrollment.getAcademicYearId(), enrollment.getClassId())
                .subtract(nz(student.getConcessionAmount()))
                .max(BigDecimal.ZERO);

        BigDecimal total = BigDecimal.ZERO;
        for (YearMonth ym = from; !ym.isAfter(to); ym = ym.plusMonths(1)) {
            boolean paid = paymentTransactionRepository.existsByStudentIdAndCollectionMonthAndCollectionYearAndStatus(
                    student.getId(), ym.getMonthValue(), ym.getYear(), "CONFIRMED");
            if (!paid) {
                total = total.add(monthlyFee);
            }
        }
        // Cleared to zero by the first payment of the year, so still > 0 means unpaid
        return total.add(nz(enrollment.getPreviousDues()));
    }

    // ── Validation ───────────────────────────────────────────────────────────

    private void validateNewYear(PromotionRequest request, UUID schoolId, CurrentYear current) {
        String name = request.getNewAcademicYearName() == null ? "" : request.getNewAcademicYearName().trim();
        if (name.isEmpty() || name.length() > 20) {
            throw new BusinessException("Enter a name for the new academic year (up to 20 characters).");
        }
        if (request.getNewStartDate() == null || request.getNewEndDate() == null) {
            throw new BusinessException("Enter start and end dates for the new academic year.");
        }
        if (!request.getNewEndDate().isAfter(request.getNewStartDate())) {
            throw new BusinessException("The new academic year must end after it starts.");
        }
        if (!request.getNewStartDate().isAfter(current.startDate())) {
            throw new BusinessException("The new academic year must start after the current one.");
        }
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_years WHERE school_id = ? AND LOWER(name) = LOWER(?)",
                Integer.class, schoolId, name);
        if (exists != null && exists > 0) {
            // Also stops a double-submitted promotion from running twice
            throw new BusinessException("An academic year named \"" + name + "\" already exists.");
        }
    }

    private void validateTarget(StudentPromotionDto p, UUID schoolId, Student student) {
        if (p.getNextClassId() == null || p.getNextSectionId() == null) {
            throw new BusinessException("Choose the next class and section for " + student.getFirstName() + ".");
        }
        SchoolClass cls = classRepository.findById(p.getNextClassId())
                .filter(c -> c.getSchoolId().equals(schoolId) && c.isActive())
                .orElseThrow(() -> new BusinessException("Invalid class selected for " + student.getFirstName() + "."));
        sectionRepository.findById(p.getNextSectionId())
                .filter(s -> s.getSchoolId().equals(schoolId) && s.isActive() && s.getClassId().equals(cls.getId()))
                .orElseThrow(() -> new BusinessException("Invalid section selected for " + student.getFirstName() + "."));
    }

    private CurrentYear currentYear(UUID schoolId) {
        List<CurrentYear> rows = jdbcTemplate.query(
                "SELECT id, start_date, end_date FROM academic_years WHERE school_id = ? AND is_current = true",
                (rs, i) -> new CurrentYear((UUID) rs.getObject("id"),
                        rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate()),
                schoolId);
        if (rows.isEmpty()) throw new BusinessException("No current academic year is configured.");
        return rows.get(0);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
