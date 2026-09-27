package com.schoolerp.modules.student.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class StudentCreateRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be at most 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be at most 50 characters")
    private String lastName;

    @PastOrPresent(message = "Date of birth cannot be in the future")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)?$", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;

    @Size(max = 100, message = "Father's name must be at most 100 characters")
    private String fatherName;
    @Size(max = 100, message = "Mother's name must be at most 100 characters")
    private String motherName;
    @Size(max = 100, message = "Guardian's name must be at most 100 characters")
    private String guardianName;

    @NotBlank(message = "Primary contact phone is required")
    @Pattern(regexp = "^[0-9+ -]{7,15}$", message = "Enter a valid phone number")
    private String primaryContactPhone;

    @Pattern(regexp = "^$|^[0-9+ -]{7,15}$", message = "Enter a valid alternate phone number")
    private String alternatePhone;
    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;
    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    private boolean staffChild = false;
    @DecimalMin(value = "0", message = "Concession cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Enter a valid concession amount")
    private BigDecimal concessionAmount = BigDecimal.ZERO;
    @Size(max = 500, message = "Concession note must be at most 500 characters")
    private String concessionNote;

    // Enrollment details
    @NotNull(message = "Class is required")
    private UUID classId;

    @NotNull(message = "Section is required")
    private UUID sectionId;

    private LocalDate enrollmentDate;

    // Optional: override admission number (leave null for auto-generate)
    @Pattern(regexp = "^$|^[A-Za-z0-9/-]{1,20}$", message = "Admission number can have up to 20 letters, digits, / or -")
    private String admissionNumber;
}
