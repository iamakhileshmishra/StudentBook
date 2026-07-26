package com.schoolerp.modules.fee.repository;

import com.schoolerp.modules.fee.entity.FeeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeeCategoryRepository extends JpaRepository<FeeCategory, UUID> {
    List<FeeCategory> findBySchoolIdAndActiveTrueOrderByDisplayOrderAsc(UUID schoolId);
}
