package com.schoolerp.modules.academic.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class AcademicYearDto {
    private UUID id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean current;
}
