package com.schoolerp.modules.student.service;

import com.schoolerp.modules.academic.entity.AcademicYear;
import com.schoolerp.modules.academic.entity.SchoolClass;
import com.schoolerp.modules.academic.entity.Section;
import com.schoolerp.modules.academic.repository.AcademicYearRepository;
import com.schoolerp.modules.academic.repository.ClassRepository;
import com.schoolerp.modules.academic.repository.SectionRepository;
import com.schoolerp.modules.student.dto.*;
import com.schoolerp.modules.student.entity.Student;
import com.schoolerp.modules.student.entity.StudentEnrollment;
import com.schoolerp.modules.student.repository.StudentEnrollmentRepository;
import com.schoolerp.modules.student.repository.StudentRepository;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.exception.ResourceNotFoundException;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicYearRepository yearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final com.schoolerp.modules.auth.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // ─── Search / List ────────────────────────────────────────────────────────

    public StudentPageResponse searchStudents(String query, UUID classId, int page, int size) {
        UUID schoolId = TenantContext.getSchoolId();
        UUID currentYearId = getCurrentYearId(schoolId);

        int offset = page * size;

        StringBuilder sql = new StringBuilder("""
                SELECT
                    s.id, s.admission_number, s.first_name, s.last_name,
                    s.father_name, s.primary_contact_phone, s.status,
                    s.is_staff_child, s.concession_amount,
                    c.name AS class_name, c.id AS class_id,
                    sec.name AS section_name,
                    se.id AS enrollment_id
                FROM students s
                LEFT JOIN student_enrollments se
                    ON se.student_id = s.id AND se.academic_year_id = ?
                LEFT JOIN classes c ON c.id = se.class_id
                LEFT JOIN sections sec ON sec.id = se.section_id
                WHERE s.school_id = ?
                """);

        List<Object> params = new ArrayList<>(Arrays.asList(currentYearId, schoolId));

        if (StringUtils.hasText(query)) {
            sql.append(" AND (LOWER(s.first_name || ' ' || s.last_name) LIKE LOWER(?) OR s.admission_number LIKE ?)");
            params.add("%" + query.trim() + "%");
            params.add("%" + query.trim() + "%");
        }

        if (classId != null) {
            sql.append(" AND se.class_id = ?");
            params.add(classId);
        }

        // Count queries
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS cnt";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        if (total == null) total = 0L;

        String activeCountSql = "SELECT COUNT(*) FROM (" + sql + " AND s.status = 'ACTIVE') AS cnt";
        Long activeCount = jdbcTemplate.queryForObject(activeCountSql, Long.class, params.toArray());
        if (activeCount == null) activeCount = 0L;

        // Paged query
        sql.append(" ORDER BY (CASE WHEN s.admission_number ~ '^[0-9]+$' THEN CAST(s.admission_number AS INTEGER) ELSE 99999 END) ASC");
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        List<StudentSummaryDto> content = jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> StudentSummaryDto.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .admissionNumber(rs.getString("admission_number"))
                        .firstName(rs.getString("first_name"))
                        .lastName(rs.getString("last_name"))
                        .fullName(rs.getString("first_name") + " " + rs.getString("last_name"))
                        .fatherName(rs.getString("father_name"))
                        .primaryContactPhone(rs.getString("primary_contact_phone"))
                        .status(rs.getString("status"))
                        .staffChild(rs.getBoolean("is_staff_child"))
                        .concessionAmount(rs.getBigDecimal("concession_amount"))
                        .className(rs.getString("class_name"))
                        .sectionName(rs.getString("section_name"))
                        .classId(rs.getString("class_id") != null ? UUID.fromString(rs.getString("class_id")) : null)
                        .enrollmentId(rs.getString("enrollment_id") != null ? UUID.fromString(rs.getString("enrollment_id")) : null)
                        .build()
        );

        int totalPages = (int) Math.ceil((double) total / size);
        return StudentPageResponse.builder()
                .content(content)
                .totalElements(total)
                .activeCount(activeCount)
                .totalPages(totalPages)
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    // ─── Get single student ───────────────────────────────────────────────────

    public StudentDetailDto getStudent(UUID studentId) {
        UUID schoolId = TenantContext.getSchoolId();
        Student student = studentRepository.findById(studentId)
                .filter(s -> s.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        UUID currentYearId = getCurrentYearId(schoolId);
        Optional<StudentEnrollment> enrollment =
                enrollmentRepository.findByStudentIdAndAcademicYearIdAndSchoolId(studentId, currentYearId, schoolId);

        StudentDetailDto.StudentDetailDtoBuilder builder = StudentDetailDto.builder()
                .id(student.getId())
                .admissionNumber(student.getAdmissionNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .fullName(student.getFirstName() + " " + student.getLastName())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .fatherName(student.getFatherName())
                .motherName(student.getMotherName())
                .guardianName(student.getGuardianName())
                .primaryContactPhone(student.getPrimaryContactPhone())
                .alternatePhone(student.getAlternatePhone())
                .email(student.getEmail())
                .address(student.getAddress())
                .staffChild(student.isStaffChild())
                .concessionAmount(student.getConcessionAmount())
                .concessionNote(student.getConcessionNote())
                .status(student.getStatus())
                .createdAt(student.getCreatedAt());

        enrollment.ifPresent(en -> {
            builder.enrollmentId(en.getId())
                   .classId(en.getClassId())
                   .sectionId(en.getSectionId())
                   .enrollmentDate(en.getEnrollmentDate())
                   .previousDues(en.getPreviousDues());

            classRepository.findById(en.getClassId())
                    .ifPresent(c -> builder.className(c.getName()));
            sectionRepository.findById(en.getSectionId())
                    .ifPresent(s -> builder.sectionName(s.getName()));
            yearRepository.findById(en.getAcademicYearId())
                    .ifPresent(y -> builder.academicYearName(y.getName()));
        });

        return builder.build();
    }

    // ─── Create student ───────────────────────────────────────────────────────

    @Transactional
    public StudentDetailDto createStudent(StudentCreateRequest req, UUID createdByUserId) {
        UUID schoolId = TenantContext.getSchoolId();

        // Validate class and section belong to this school
        SchoolClass schoolClass = classRepository.findById(req.getClassId())
                .filter(c -> c.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new BusinessException("Invalid class selected"));

        Section section = sectionRepository.findById(req.getSectionId())
                .filter(s -> s.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new BusinessException("Invalid section selected"));

        // Generate or validate admission number
        String admissionNumber;
        if (StringUtils.hasText(req.getAdmissionNumber())) {
            String sanitizedAdm = req.getAdmissionNumber().trim().replaceFirst("^0+(?!$)", "");
            if (studentRepository.existsBySchoolIdAndAdmissionNumber(schoolId, sanitizedAdm)) {
                throw new BusinessException("Admission number " + sanitizedAdm + " is already taken");
            }
            admissionNumber = sanitizedAdm;
        } else {
            admissionNumber = generateNextAdmissionNumber(schoolId);
        }

        // Get current academic year
        AcademicYear currentYear = yearRepository.findCurrentBySchoolId(schoolId)
                .orElseThrow(() -> new BusinessException("No current academic year. Please configure one first."));

        // Save student
        Student student = Student.builder()
                .schoolId(schoolId)
                .admissionNumber(admissionNumber)
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .dateOfBirth(req.getDateOfBirth())
                .gender(req.getGender())
                .fatherName(req.getFatherName())
                .motherName(req.getMotherName())
                .guardianName(req.getGuardianName())
                .primaryContactPhone(req.getPrimaryContactPhone())
                .alternatePhone(req.getAlternatePhone())
                .email(req.getEmail())
                .address(req.getAddress())
                .staffChild(req.isStaffChild())
                .concessionAmount(req.getConcessionAmount() != null ? req.getConcessionAmount() : BigDecimal.ZERO)
                .concessionNote(req.getConcessionNote())
                .status("ACTIVE")
                .createdBy(createdByUserId)
                .build();
        student = studentRepository.save(student);

        // Save enrollment
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .schoolId(schoolId)
                .studentId(student.getId())
                .academicYearId(currentYear.getId())
                .classId(req.getClassId())
                .sectionId(req.getSectionId())
                .enrollmentDate(req.getEnrollmentDate() != null ? req.getEnrollmentDate() : LocalDate.now())
                .status("ACTIVE")
                .build();
        enrollment = enrollmentRepository.save(enrollment);

        log.info("Created student {} ({}) in class {}",
                student.getAdmissionNumber(), student.getFirstName(), schoolClass.getName());

        return StudentDetailDto.builder()
                .id(student.getId())
                .admissionNumber(student.getAdmissionNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .fullName(student.getFirstName() + " " + student.getLastName())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .fatherName(student.getFatherName())
                .motherName(student.getMotherName())
                .primaryContactPhone(student.getPrimaryContactPhone())
                .address(student.getAddress())
                .staffChild(student.isStaffChild())
                .concessionAmount(student.getConcessionAmount())
                .status(student.getStatus())
                .enrollmentId(enrollment.getId())
                .classId(enrollment.getClassId())
                .className(schoolClass.getName())
                .sectionId(enrollment.getSectionId())
                .sectionName(section.getName())
                .academicYearName(currentYear.getName())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .createdAt(student.getCreatedAt())
                .build();
    }

    // ─── Update student ───────────────────────────────────────────────────────

    @Transactional
    public StudentDetailDto updateStudent(UUID studentId, UpdateStudentRequest req) {
        UUID schoolId = TenantContext.getSchoolId();
        Student student = studentRepository.findById(studentId)
                .filter(s -> s.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        student.setFirstName(req.getFirstName().trim());
        student.setLastName(req.getLastName().trim());
        student.setDateOfBirth(req.getDateOfBirth());
        student.setGender(req.getGender());
        student.setFatherName(req.getFatherName());
        student.setMotherName(req.getMotherName());
        student.setGuardianName(req.getGuardianName());
        student.setPrimaryContactPhone(req.getPrimaryContactPhone());
        student.setAlternatePhone(req.getAlternatePhone());
        student.setEmail(req.getEmail());
        student.setAddress(req.getAddress());
        student.setStaffChild(req.isStaffChild());
        student.setConcessionAmount(req.getConcessionAmount() != null ? req.getConcessionAmount() : BigDecimal.ZERO);
        student.setConcessionNote(req.getConcessionNote());
        studentRepository.save(student);

        return getStudent(studentId);
    }
    
    // ─── Delete student ───────────────────────────────────────────────────────

    @Transactional
    public void deleteStudent(UUID studentId, String adminPassword, UUID adminUserId) {
        UUID schoolId = TenantContext.getSchoolId();
        
        com.schoolerp.modules.auth.entity.User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new BusinessException("Admin user not found"));
                
        if (!passwordEncoder.matches(adminPassword, admin.getPasswordHash())) {
            throw new BusinessException("Invalid admin password");
        }
        
        Student student = studentRepository.findById(studentId)
                .filter(s -> s.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
                
        student.setStatus("LEFT");
        studentRepository.save(student);
        
        UUID currentYearId = getCurrentYearId(schoolId);
        enrollmentRepository.findByStudentIdAndAcademicYearIdAndSchoolId(studentId, currentYearId, schoolId)
                .ifPresent(en -> {
                    en.setStatus("LEFT");
                    enrollmentRepository.save(en);
                });
                
        log.info("Student {} ({}) has been marked as LEFT by admin {}", student.getAdmissionNumber(), student.getFirstName(), admin.getEmail());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String generateNextAdmissionNumber(UUID schoolId) {
        int max = studentRepository.findMaxAdmissionNumber(schoolId);
        return String.valueOf(max + 1);
    }

    private UUID getCurrentYearId(UUID schoolId) {
        return yearRepository.findCurrentBySchoolId(schoolId)
                .map(AcademicYear::getId)
                .orElseThrow(() -> new BusinessException("No current academic year configured"));
    }
}
