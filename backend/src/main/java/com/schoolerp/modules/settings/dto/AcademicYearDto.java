package com.schoolerp.modules.settings.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AcademicYearDto {
    private UUID id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
}
