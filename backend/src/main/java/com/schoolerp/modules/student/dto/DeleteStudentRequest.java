package com.schoolerp.modules.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteStudentRequest {
    @NotBlank(message = "Password is required to confirm deletion")
    private String password;
}
