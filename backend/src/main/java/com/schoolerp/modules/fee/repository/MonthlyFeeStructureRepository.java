package com.schoolerp.modules.fee.repository;

import com.schoolerp.modules.fee.entity.MonthlyFeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthlyFeeStructureRepository extends JpaRepository<MonthlyFeeStructure, UUID> {

    List<MonthlyFeeStructure> findBySchoolIdAndAcademicYearIdAndClassIdOrderByFeeCategoryId(
            UUID schoolId, UUID academicYearId, UUID classId);

    Optional<MonthlyFeeStructure> findBySchoolIdAndAcademicYearIdAndClassIdAndFeeCategoryId(
            UUID schoolId, UUID academicYearId, UUID classId, UUID feeCategoryId);

    @Modifying
    @Query("""
        UPDATE MonthlyFeeStructure m
        SET m.defaultAmount = :amount, m.updatedBy = :updatedBy, m.updatedAt = CURRENT_TIMESTAMP
        WHERE m.schoolId = :schoolId AND m.academicYearId = :academicYearId
              AND m.classId = :classId AND m.feeCategoryId = :feeCategoryId""")
    int updateAmount(@Param("schoolId") UUID schoolId,
                     @Param("academicYearId") UUID academicYearId,
                     @Param("classId") UUID classId,
                     @Param("feeCategoryId") UUID feeCategoryId,
                     @Param("amount") BigDecimal amount,
                     @Param("updatedBy") UUID updatedBy);
}
