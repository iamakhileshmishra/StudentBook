package com.schoolerp.modules.settings.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SchoolProfileDto {
    @NotBlank
    private String name;
    
    private String shortName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String phone;
    private String email;
    private String website;
    private String logoUrl;
    
    private Integer feeDueDate;
    private List<LateFeeRuleDto> lateFeeRules;
}
