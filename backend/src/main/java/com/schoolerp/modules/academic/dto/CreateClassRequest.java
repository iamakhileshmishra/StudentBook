package com.schoolerp.modules.academic.dto;

import lombok.Data;

@Data
public class CreateClassRequest {
    private String name;
    private Integer numericOrder;
}
