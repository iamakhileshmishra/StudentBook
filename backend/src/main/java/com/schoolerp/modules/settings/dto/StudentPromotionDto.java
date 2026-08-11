package com.schoolerp.modules.settings.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class StudentPromotionDto {
    private UUID studentId;
    private PromotionDecision decision;
    private UUID nextClassId;
    private UUID nextSectionId; // Optional, can be null if assigning later
}
