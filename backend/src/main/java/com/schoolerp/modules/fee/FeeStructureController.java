package com.schoolerp.modules.fee;

import com.schoolerp.modules.fee.dto.*;
import com.schoolerp.modules.fee.service.FeeStructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fee-structure")
@RequiredArgsConstructor
public class FeeStructureController {

    private final FeeStructureService service;

    @GetMapping("/categories")
    public ResponseEntity<List<FeeCategoryDto>> getCategories() {
        return ResponseEntity.ok(service.getCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<FeeCategoryDto> createCategory(@Valid @RequestBody CreateFeeCategoryRequest request) {
        return ResponseEntity.ok(service.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<FeeCategoryDto> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CreateFeeCategoryRequest request) {
        return ResponseEntity.ok(service.updateCategory(id, request));
    }

    @GetMapping
    public ResponseEntity<List<ClassFeeStructureDto>> getAllClassFeeStructures() {
        return ResponseEntity.ok(service.getFeeStructureForCurrentYear());
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ClassFeeStructureDto> getClassFeeStructure(@PathVariable UUID classId) {
        return ResponseEntity.ok(service.getFeeStructureForClass(classId));
    }

    @PutMapping("/update")
    public ResponseEntity<FeeStructureRowDto> updateFeeAmount(
            @Valid @RequestBody UpdateFeeAmountRequest request) {
        return ResponseEntity.ok(service.updateFeeAmount(request));
    }
}
