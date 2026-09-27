package com.schoolerp.modules.fee.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/** Validated so client-supplied amounts can't produce negative or broken payments (BUG-03). */
@Data
public class CollectFeeRequest {
    @NotNull(message = "Student is required")
    private UUID studentId;

    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private int month;

    @Min(value = 2000, message = "Enter a valid year")
    @Max(value = 2100, message = "Enter a valid year")
    private int year;

    @DecimalMin(value = "0", message = "Concession cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Enter a valid concession amount")
    private BigDecimal concessionAmount;

    @DecimalMin(value = "0", message = "Late fee cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Enter a valid late fee")
    private BigDecimal lateFee;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "CASH|UPI|CHEQUE|BANK_TRANSFER|OTHER", message = "Invalid payment method")
    private String paymentMethod;

    @Size(max = 100, message = "Payment reference is too long")
    private String paymentReference;

    @Size(max = 500, message = "Remarks must be at most 500 characters")
    private String remarks;
}
