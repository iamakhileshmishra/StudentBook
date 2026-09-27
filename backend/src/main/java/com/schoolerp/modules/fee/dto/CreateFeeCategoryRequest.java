package com.schoolerp.modules.fee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateFeeCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;
}
