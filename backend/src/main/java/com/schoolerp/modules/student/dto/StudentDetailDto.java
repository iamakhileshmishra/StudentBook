package com.schoolerp.modules.student.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class StudentDetailDto {
    // Identity
    private UUID id;
    private String admissionNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;

    // Parent
    private String fatherName;
    private String motherName;
    private String guardianName;
    private String primaryContactPhone;
    private String alternatePhone;
    private String email;
    private String address;

    // Concession
    private boolean staffChild;
    private BigDecimal concessionAmount;
    private String concessionNote;

    // Status
    private String status;
    private Instant createdAt;

    // Current enrollment
    private UUID enrollmentId;
    private UUID classId;
    private String className;
    private UUID sectionId;
    private String sectionName;
    private String academicYearName;
    private LocalDate enrollmentDate;
    private BigDecimal previousDues;
}
