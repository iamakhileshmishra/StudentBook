package com.schoolerp.modules.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateStudentRequest {
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    private LocalDate dateOfBirth;
    private String gender;
    
    private String fatherName;
    private String motherName;
    private String guardianName;
    
    @NotBlank(message = "Primary contact phone is required")
    private String primaryContactPhone;
    
    private String alternatePhone;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String address;
    
    private boolean staffChild;
    
    @NotNull(message = "Concession amount is required")
    private BigDecimal concessionAmount;
    
    private String concessionNote;
}
