package com.schoolerp.modules.settings.controller;

import com.schoolerp.modules.settings.dto.ChangePasswordRequest;
import com.schoolerp.modules.settings.dto.SchoolProfileDto;
import com.schoolerp.modules.settings.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import com.schoolerp.modules.settings.dto.AcademicYearDto;
import com.schoolerp.modules.settings.dto.PromotionDataResponse;
import com.schoolerp.modules.settings.dto.PromotionRequest;
import com.schoolerp.modules.settings.service.AcademicYearPromotionService;
import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final AcademicYearPromotionService promotionService;

    @GetMapping("/school")
    public ResponseEntity<SchoolProfileDto> getSchoolProfile() {
        return ResponseEntity.ok(settingsService.getSchoolProfile());
    }

    @PutMapping("/school")
    public ResponseEntity<SchoolProfileDto> updateSchoolProfile(@Valid @RequestBody SchoolProfileDto request) {
        return ResponseEntity.ok(settingsService.updateSchoolProfile(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID currentUserId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        settingsService.changePassword(request, currentUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/academic-years/current")
    public ResponseEntity<AcademicYearDto> getCurrentAcademicYear() {
        return ResponseEntity.ok(settingsService.getCurrentAcademicYear());
    }

    @PutMapping("/academic-years/current")
    public ResponseEntity<AcademicYearDto> updateCurrentAcademicYear(@RequestBody AcademicYearDto request) {
        return ResponseEntity.ok(settingsService.updateCurrentAcademicYear(request));
    }

    @GetMapping("/academic-years/promotion-data")
    public ResponseEntity<PromotionDataResponse> getPromotionData() {
        return ResponseEntity.ok(promotionService.getPromotionData());
    }

    @PostMapping("/academic-years/promote")
    public ResponseEntity<Void> promoteStudents(@RequestBody PromotionRequest request) {
        promotionService.promoteStudents(request);
        return ResponseEntity.ok().build();
    }
}
