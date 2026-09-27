package com.schoolerp.modules.settings.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Validated to match the sign-up form and the column sizes (test&bug.md BUG-16). */
@Data
@Builder
public class SchoolProfileDto {
    @NotBlank(message = "School name is required")
    @Size(max = 200, message = "School name must be at most 200 characters")
    private String name;

    @Pattern(regexp = "^[A-Za-z0-9]{0,10}$", message = "Short name can have up to 10 letters and numbers")
    private String shortName;

    @Size(max = 255, message = "Address line 1 must be at most 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must be at most 255 characters")
    private String addressLine2;

    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    @Size(max = 100, message = "State must be at most 100 characters")
    private String state;

    @Pattern(regexp = "^$|^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    @Pattern(regexp = "^$|^[0-9+ -]{7,15}$", message = "Enter a valid phone number")
    private String phone;

    // Password reset and change-password codes are sent here, so it can't be removed
    @NotBlank(message = "School email is required — it is used for password recovery")
    @Email(message = "Enter a valid email address")
    @Size(max = 100)
    private String email;

    @Size(max = 200, message = "Website must be at most 200 characters")
    private String website;

    @Size(max = 500)
    private String logoUrl;

    @Min(value = 1, message = "Fee due date must be between 1 and 28")
    @Max(value = 28, message = "Fee due date must be between 1 and 28")
    private Integer feeDueDate;

    @Valid
    @Size(max = 20, message = "At most 20 late fee rules")
    private List<LateFeeRuleDto> lateFeeRules;
}
