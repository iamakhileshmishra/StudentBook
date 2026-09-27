package com.schoolerp.modules.auth.dto;

import lombok.Builder;
import lombok.Data;

/** The temporary password is only ever sent by email, never returned here. */
@Data
@Builder
public class RegisterSchoolResponse {
    private String schoolName;
    private String email;
}
