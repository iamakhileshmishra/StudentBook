package com.schoolerp.modules.student.repository;

import com.schoolerp.modules.student.entity.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {
    Optional<StudentEnrollment> findByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);
    Optional<StudentEnrollment> findByStudentIdAndAcademicYearIdAndSchoolId(UUID studentId, UUID academicYearId, UUID schoolId);
    long countBySchoolIdAndAcademicYearIdAndStatus(UUID schoolId, UUID academicYearId, String status);
}
