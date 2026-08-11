package com.schoolerp.modules.fee.service;

import com.schoolerp.modules.academic.entity.SchoolClass;
import com.schoolerp.modules.academic.repository.AcademicYearRepository;
import com.schoolerp.modules.academic.repository.ClassRepository;
import com.schoolerp.modules.fee.dto.*;
import com.schoolerp.modules.fee.entity.FeeCategory;
import com.schoolerp.modules.fee.entity.MonthlyFeeStructure;
import com.schoolerp.modules.fee.repository.FeeCategoryRepository;
import com.schoolerp.modules.fee.repository.MonthlyFeeStructureRepository;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeStructureService {

    private final FeeCategoryRepository categoryRepository;
    private final MonthlyFeeStructureRepository structureRepository;
    private final AcademicYearRepository yearRepository;
    private final ClassRepository classRepository;

    // ─── Get fee categories ─────────────────────────────────────────────────

    public List<FeeCategoryDto> getCategories() {
        UUID schoolId = TenantContext.getSchoolId();
        return categoryRepository.findBySchoolIdAndActiveTrueOrderByDisplayOrderAsc(schoolId)
                .stream()
                .map(c -> FeeCategoryDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .displayOrder(c.getDisplayOrder())
                        .active(c.isActive())
                        .build())
                .toList();
    }

    // ─── Get fee structure for all classes (current year) ───────────────────

    public List<ClassFeeStructureDto> getFeeStructureForCurrentYear() {
        UUID schoolId = TenantContext.getSchoolId();
        UUID yearId = getCurrentYearId(schoolId);

        List<SchoolClass> classes = classRepository
                .findBySchoolIdAndActiveTrueOrderByNumericOrderAsc(schoolId);

        List<FeeCategory> categories = categoryRepository
                .findBySchoolIdAndActiveTrueOrderByDisplayOrderAsc(schoolId);

        Map<UUID, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(FeeCategory::getId, FeeCategory::getName));
        Map<UUID, Integer> categoryOrders = categories.stream()
                .collect(Collectors.toMap(FeeCategory::getId, FeeCategory::getDisplayOrder));

        List<ClassFeeStructureDto> result = new ArrayList<>();

        for (SchoolClass cls : classes) {
            List<MonthlyFeeStructure> rows = structureRepository
                    .findBySchoolIdAndAcademicYearIdAndClassIdOrderByFeeCategoryId(
                            schoolId, yearId, cls.getId());

            List<FeeStructureRowDto> rowDtos = categories.stream()
                    .map(cat -> {
                        MonthlyFeeStructure existing = rows.stream()
                                .filter(r -> r.getFeeCategoryId().equals(cat.getId()))
                                .findFirst()
                                .orElse(null);
                        
                        return FeeStructureRowDto.builder()
                                .structureId(existing != null ? existing.getId() : null)
                                .feeCategoryId(cat.getId())
                                .categoryName(cat.getName())
                                .displayOrder(cat.getDisplayOrder())
                                .defaultAmount(existing != null ? existing.getDefaultAmount() : BigDecimal.ZERO)
                                .build();
                    })
                    .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                    .toList();

            BigDecimal total = rowDtos.stream()
                    .map(FeeStructureRowDto::getDefaultAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(ClassFeeStructureDto.builder()
                    .classId(cls.getId())
                    .className(cls.getName())
                    .rows(rowDtos)
                    .totalMonthlyFee(total)
                    .build());
        }
        return result;
    }

    // ─── Get fee structure for a specific class ──────────────────────────────

    public ClassFeeStructureDto getFeeStructureForClass(UUID classId) {
        UUID schoolId = TenantContext.getSchoolId();
        UUID yearId = getCurrentYearId(schoolId);

        SchoolClass cls = classRepository.findById(classId)
                .filter(c -> c.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new BusinessException("Invalid class"));

        List<FeeCategory> categories = categoryRepository
                .findBySchoolIdAndActiveTrueOrderByDisplayOrderAsc(schoolId);
        Map<UUID, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(FeeCategory::getId, FeeCategory::getName));
        Map<UUID, Integer> categoryOrders = categories.stream()
                .collect(Collectors.toMap(FeeCategory::getId, FeeCategory::getDisplayOrder));

        List<MonthlyFeeStructure> rows = structureRepository
                .findBySchoolIdAndAcademicYearIdAndClassIdOrderByFeeCategoryId(
                        schoolId, yearId, classId);

        List<FeeStructureRowDto> rowDtos = categories.stream()
                .map(cat -> {
                    MonthlyFeeStructure existing = rows.stream()
                            .filter(r -> r.getFeeCategoryId().equals(cat.getId()))
                            .findFirst()
                            .orElse(null);
                    
                    return FeeStructureRowDto.builder()
                            .structureId(existing != null ? existing.getId() : null)
                            .feeCategoryId(cat.getId())
                            .categoryName(cat.getName())
                            .displayOrder(cat.getDisplayOrder())
                            .defaultAmount(existing != null ? existing.getDefaultAmount() : BigDecimal.ZERO)
                            .build();
                })
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                .toList();

        BigDecimal total = rowDtos.stream()
                .map(FeeStructureRowDto::getDefaultAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ClassFeeStructureDto.builder()
                .classId(cls.getId())
                .className(cls.getName())
                .rows(rowDtos)
                .totalMonthlyFee(total)
                .build();
    }

    // ─── Update fee amount ───────────────────────────────────────────────────

    @Transactional
    public FeeStructureRowDto updateFeeAmount(UpdateFeeAmountRequest req) {
        UUID schoolId = TenantContext.getSchoolId();
        UUID yearId = getCurrentYearId(schoolId);
        UUID updatedBy = getCurrentUserId();

        // Upsert: update if exists, insert if not
        int updated = structureRepository.updateAmount(
                schoolId, yearId, req.getClassId(), req.getFeeCategoryId(),
                req.getAmount(), updatedBy);

        if (updated == 0) {
            // Row doesn't exist yet — create it
            MonthlyFeeStructure newRow = MonthlyFeeStructure.builder()
                    .schoolId(schoolId)
                    .academicYearId(yearId)
                    .classId(req.getClassId())
                    .feeCategoryId(req.getFeeCategoryId())
                    .defaultAmount(req.getAmount())
                    .updatedBy(updatedBy)
                    .build();
            structureRepository.save(newRow);
        }

        // Return updated row
        FeeCategory cat = categoryRepository.findById(req.getFeeCategoryId())
                .orElseThrow(() -> new BusinessException("Fee category not found"));

        MonthlyFeeStructure saved = structureRepository
                .findBySchoolIdAndAcademicYearIdAndClassIdAndFeeCategoryId(
                        schoolId, yearId, req.getClassId(), req.getFeeCategoryId())
                .orElseThrow();

        return FeeStructureRowDto.builder()
                .structureId(saved.getId())
                .feeCategoryId(cat.getId())
                .categoryName(cat.getName())
                .displayOrder(cat.getDisplayOrder())
                .defaultAmount(saved.getDefaultAmount())
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private UUID getCurrentYearId(UUID schoolId) {
        return yearRepository.findCurrentBySchoolId(schoolId)
                .map(y -> y.getId())
                .orElseThrow(() -> new BusinessException("No current academic year configured"));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try { return UUID.fromString(auth.getName()); } catch (Exception ignored) {}
        }
        return null;
    }
}
