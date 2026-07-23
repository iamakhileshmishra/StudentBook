package com.schoolerp.modules.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class StudentCreateRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private LocalDate dateOfBirth;

    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;

    private String fatherName;
    private String motherName;
    private String guardianName;

    @NotBlank(message = "Primary contact phone is required")
    private String primaryContactPhone;

    private String alternatePhone;
    private String email;
    private String address;

    private boolean staffChild = false;
    private BigDecimal concessionAmount = BigDecimal.ZERO;
    private String concessionNote;

    // Enrollment details
    @NotNull(message = "Class is required")
    private UUID classId;

    @NotNull(message = "Section is required")
    private UUID sectionId;

    private LocalDate enrollmentDate;

    // Optional: override admission number (leave null for auto-generate)
    private String admissionNumber;
}
