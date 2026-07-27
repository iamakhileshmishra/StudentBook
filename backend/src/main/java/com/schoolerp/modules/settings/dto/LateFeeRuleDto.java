package com.schoolerp.modules.settings.dto;

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
    private int startDaysLate;
    private int endDaysLate;
    private BigDecimal amount;
}
