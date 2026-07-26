package com.schoolerp.modules.fee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UpdateFeeAmountRequest {
    @NotNull
    private UUID classId;

    @NotNull
    private UUID feeCategoryId;

    @NotNull
    @DecimalMin(value = "0", message = "Amount cannot be negative")
    private BigDecimal amount;
}
