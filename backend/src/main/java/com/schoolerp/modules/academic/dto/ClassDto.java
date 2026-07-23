package com.schoolerp.modules.academic.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ClassDto {
    private UUID id;
    private String name;
    private int numericOrder;
    private List<SectionDto> sections;
}
