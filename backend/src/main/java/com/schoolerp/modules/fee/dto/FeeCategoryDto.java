package com.schoolerp.modules.fee.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FeeCategoryDto {
    private UUID id;
    private String name;
    private int displayOrder;
    private boolean active;
}
