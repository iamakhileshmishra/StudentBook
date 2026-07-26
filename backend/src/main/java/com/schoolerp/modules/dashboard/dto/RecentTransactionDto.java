package com.schoolerp.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentTransactionDto {
    private UUID id;
    private String receiptNumber;
    private String studentName;
    private String admissionNumber;
    private String className;
    private BigDecimal amountPaid;
    private String paymentMethod;
    private Instant confirmedAt;
}
