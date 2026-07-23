package com.schoolerp.modules.student.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentPageResponse {
    private List<StudentSummaryDto> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
