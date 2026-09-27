package com.schoolerp.modules.dashboard.controller;

import com.schoolerp.modules.dashboard.dto.ClassWiseStatDto;
import com.schoolerp.modules.dashboard.dto.DashboardStudentDto;
import com.schoolerp.modules.dashboard.dto.DashboardSummaryDto;
import com.schoolerp.modules.dashboard.dto.RecentTransactionDto;
import com.schoolerp.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary(
            @RequestParam int month,
            @RequestParam int year) {
        validatePeriod(month, year);
        return ResponseEntity.ok(dashboardService.getSummary(month, year));
    }

    @GetMapping("/class-wise")
    public ResponseEntity<List<ClassWiseStatDto>> getClassWiseStats(
            @RequestParam int month,
            @RequestParam int year) {
        validatePeriod(month, year);
        return ResponseEntity.ok(dashboardService.getClassWiseStats(month, year));
    }

    @GetMapping("/class-students")
    public ResponseEntity<List<DashboardStudentDto>> getClassStudents(
            @RequestParam UUID classId,
            @RequestParam int month,
            @RequestParam int year) {
        validatePeriod(month, year);
        return ResponseEntity.ok(dashboardService.getClassStudents(classId, month, year));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RecentTransactionDto>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentTransactions(limit));
    }

    private static void validatePeriod(int month, int year) {
        if (month < 1 || month > 12 || year < 2000 || year > 2100) {
            throw new com.schoolerp.shared.exception.BusinessException("Enter a valid month (1-12) and year.");
        }
    }
}
