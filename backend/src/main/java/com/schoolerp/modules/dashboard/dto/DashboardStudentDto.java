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
public class DashboardStudentDto {
    private String studentId;
    private String name;
    private String admissionNumber;
    private String paymentStatus; // PAID, PARTIAL, UNPAID
    private BigDecimal amountPaid;
}
