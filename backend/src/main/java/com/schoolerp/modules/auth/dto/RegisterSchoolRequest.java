package com.schoolerp.modules.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Public sign-up for a new school. Fields mirror the Settings → School Profile
 * form so everything entered here shows up there after first login.
 */
@Data
public class RegisterSchoolRequest {

    // ── Owner / first admin ────────────────────────────────────
    @NotBlank(message = "Your name is required")
    @Size(max = 100)
    private String ownerName;

    // ── School profile ─────────────────────────────────────────
    @NotBlank(message = "School name is required")
    @Size(max = 200)
    private String schoolName;

    @Size(max = 10, message = "Short name must be at most 10 characters")
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "Short name can only contain letters and numbers")
    private String shortName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phone;

    @NotBlank(message = "Address is required")
    @Size(max = 255)
    private String addressLine1;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    // ── Fee settings ───────────────────────────────────────────
    @Min(value = 1, message = "Due date must be between 1 and 28")
    @Max(value = 28, message = "Due date must be between 1 and 28")
    private Integer feeDueDate;
}
