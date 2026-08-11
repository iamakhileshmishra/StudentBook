package com.schoolerp.modules.settings.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PromotionRequest {
    private String newAcademicYearName;
    private LocalDate newStartDate;
    private LocalDate newEndDate;
    private List<StudentPromotionDto> promotions;
}
