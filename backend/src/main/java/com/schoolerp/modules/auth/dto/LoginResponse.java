package com.schoolerp.modules.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private UUID id;
        private String username;
        private String fullName;
        private String role;
        private UUID schoolId;
        private String schoolName;
    }
}
