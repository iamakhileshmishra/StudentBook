package com.schoolerp.modules.fee.service;

import com.schoolerp.modules.fee.entity.FeeCategory;
import com.schoolerp.modules.fee.entity.MonthlyFeeStructure;
import com.schoolerp.modules.fee.repository.FeeCategoryRepository;
import com.schoolerp.modules.fee.repository.MonthlyFeeStructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A class's monthly fee rows. Shared by dues, collection and promotion so they
 * always agree on what a month costs.
 */
@Component
@RequiredArgsConstructor
public class FeeStructureLookup {

    private final MonthlyFeeStructureRepository structureRepository;
    private final FeeCategoryRepository categoryRepository;

    /** Active category id → name for the school. */
    public Map<UUID, String> activeCategoryNames(UUID schoolId) {
        return categoryRepository.findBySchoolIdAndActiveTrueOrderByDisplayOrderAsc(schoolId).stream()
                .collect(Collectors.toMap(FeeCategory::getId, FeeCategory::getName));
    }

    /**
     * Fee rows for the class in that year, excluding categories that have been
     * deactivated — those used to be charged anyway and shown as "Fee" (BUG-14).
     */
    public List<MonthlyFeeStructure> activeRows(UUID schoolId, UUID academicYearId, UUID classId) {
        Map<UUID, String> active = activeCategoryNames(schoolId);
        return structureRepository
                .findBySchoolIdAndAcademicYearIdAndClassIdOrderByFeeCategoryId(schoolId, academicYearId, classId)
                .stream()
                .filter(s -> active.containsKey(s.getFeeCategoryId()))
                .toList();
    }

    public BigDecimal monthlyTotal(UUID schoolId, UUID academicYearId, UUID classId) {
        return activeRows(schoolId, academicYearId, classId).stream()
                .map(MonthlyFeeStructure::getDefaultAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
