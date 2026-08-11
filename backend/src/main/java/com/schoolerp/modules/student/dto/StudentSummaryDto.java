package com.schoolerp.modules.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSummaryDto {
    private UUID id;
    private String admissionNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String fatherName;
    private String primaryContactPhone;
    private String status;
    private boolean staffChild;
    private BigDecimal concessionAmount;
    private BigDecimal pendingDues;
    private String className;
    private String sectionName;
    private UUID classId;
    private UUID enrollmentId;
}
