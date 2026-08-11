package com.schoolerp.modules.settings.dto;

import com.schoolerp.modules.student.dto.StudentSummaryDto;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PromotionDataResponse {
    private List<StudentSummaryDto> currentStudents;
    private List<ClassData> classes;

    @Data
    @Builder
    public static class ClassData {
        private UUID id;
        private String name;
        private Integer numericOrder;
        private List<SectionData> sections;
    }

    @Data
    @Builder
    public static class SectionData {
        private UUID id;
        private String name;
    }
}
