package com.schoolerp.modules.academic.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SectionDto {
    private UUID id;
    private String name;
}
