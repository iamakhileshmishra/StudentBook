package com.schoolerp.modules.student.repository;

import com.schoolerp.modules.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findBySchoolIdAndAdmissionNumber(UUID schoolId, String admissionNumber);

    boolean existsBySchoolIdAndAdmissionNumber(UUID schoolId, String admissionNumber);

    @Query(value = """
        SELECT COALESCE(MAX(CAST(admission_number AS NUMERIC)), 0)
        FROM students
        WHERE school_id = :schoolId
        AND admission_number ~ '^[0-9]+$'
        """, nativeQuery = true)
    java.math.BigDecimal findMaxAdmissionNumber(@Param("schoolId") UUID schoolId);
}
