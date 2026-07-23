package com.schoolerp.modules.academic.repository;

import com.schoolerp.modules.academic.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassRepository extends JpaRepository<SchoolClass, UUID> {
    List<SchoolClass> findBySchoolIdAndActiveTrueOrderByNumericOrderAsc(UUID schoolId);
}
