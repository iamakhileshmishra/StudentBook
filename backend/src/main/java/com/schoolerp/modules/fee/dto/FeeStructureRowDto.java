package com.schoolerp.modules.fee.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class FeeStructureRowDto {
    private UUID structureId;    // monthly_fee_structures.id
    private UUID feeCategoryId;
    private String categoryName;
    private int displayOrder;
    private BigDecimal defaultAmount;
}
