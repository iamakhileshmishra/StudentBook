package com.schoolerp.modules.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;

    /**
     * Emailed verification code (see POST /settings/change-password/request-otp).
     * Not required on first login, when the user is replacing an emailed temporary password.
     */
    @Pattern(regexp = "^[0-9]{6}$", message = "Enter the 6-digit code")
    private String otp;
}
