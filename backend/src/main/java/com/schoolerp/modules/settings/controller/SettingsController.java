package com.schoolerp.modules.settings.controller;

import com.schoolerp.modules.settings.dto.ChangePasswordRequest;
import com.schoolerp.modules.settings.dto.SchoolProfileDto;
import com.schoolerp.modules.settings.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

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
}
