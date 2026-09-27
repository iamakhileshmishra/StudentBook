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
import com.schoolerp.modules.auth.dto.LoginResponse;
import com.schoolerp.modules.auth.service.AuthService.AuthResult;
import com.schoolerp.shared.security.RefreshCookieService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final AcademicYearPromotionService promotionService;
    private final RefreshCookieService refreshCookie;

    @GetMapping("/school")
    public ResponseEntity<SchoolProfileDto> getSchoolProfile() {
        return ResponseEntity.ok(settingsService.getSchoolProfile());
    }

    @PutMapping("/school")
    public ResponseEntity<SchoolProfileDto> updateSchoolProfile(@Valid @RequestBody SchoolProfileDto request) {
        return ResponseEntity.ok(settingsService.updateSchoolProfile(request));
    }

    @PostMapping("/change-password/request-otp")
    public ResponseEntity<Map<String, Object>> requestPasswordChangeCode(@Valid @RequestBody ChangePasswordRequest request) {
        UUID currentUserId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return ResponseEntity.ok(settingsService.requestPasswordChangeCode(request, currentUserId));
    }

    @PostMapping("/change-password")
    public ResponseEntity<LoginResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, HttpServletResponse response) {
        UUID currentUserId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        // Other sessions are signed out; this one continues with fresh tokens
        AuthResult result = settingsService.changePassword(request, currentUserId);
        refreshCookie.set(response, result.refreshToken());
        return ResponseEntity.ok(result.body());
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
