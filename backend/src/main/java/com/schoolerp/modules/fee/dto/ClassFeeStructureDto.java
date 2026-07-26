package com.schoolerp.modules.fee.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ClassFeeStructureDto {
    private UUID classId;
    private String className;
    private List<FeeStructureRowDto> rows;
    private BigDecimal totalMonthlyFee;  // sum of all default_amount > 0
}
