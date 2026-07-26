package com.schoolerp.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassWiseStatDto {
    private String classId;
    private String className;
    private long totalStudents;
    private BigDecimal collected;
    private BigDecimal pending;
}
