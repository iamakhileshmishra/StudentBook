package com.schoolerp.modules.academic.repository;

import com.schoolerp.modules.academic.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {
    List<AcademicYear> findBySchoolIdOrderByStartDateDesc(UUID schoolId);
    Optional<AcademicYear> findBySchoolIdAndCurrentTrue(UUID schoolId);
}
