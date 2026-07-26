package com.schoolerp.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {
    private BigDecimal todayCollection;
    private BigDecimal thisMonthCollection;
    private long totalStudents;
    private long studentsPaid;
    private long studentsPartiallyPaid;
    private long studentsNotPaid;
}
