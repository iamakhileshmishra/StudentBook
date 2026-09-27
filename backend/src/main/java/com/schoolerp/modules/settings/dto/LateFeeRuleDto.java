package com.schoolerp.modules.settings.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LateFeeRuleDto {
    @Min(value = 1, message = "Late fee rules must start at 1 day late or more")
    @Max(value = 366, message = "Late fee rule days must be at most 366")
    private int startDaysLate;
    @Min(value = 1, message = "Late fee rules must end at 1 day late or more")
    @Max(value = 366, message = "Late fee rule days must be at most 366")
    private int endDaysLate;
    @NotNull(message = "Late fee amount is required")
    @DecimalMin(value = "0", message = "Late fee cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Enter a valid late fee amount")
    private BigDecimal amount;
}
