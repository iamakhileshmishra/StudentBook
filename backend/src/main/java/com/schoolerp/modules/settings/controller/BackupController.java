package com.schoolerp.modules.settings.controller;

import com.schoolerp.modules.settings.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/download")
    public void downloadBackup(@RequestParam(required = false) UUID academicYearId, HttpServletResponse response) throws IOException {
        backupService.generateCsvBackupZip(academicYearId, response);
    }
}
