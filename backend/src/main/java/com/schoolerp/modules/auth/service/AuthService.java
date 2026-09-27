package com.schoolerp.modules.auth.service;

import com.schoolerp.modules.auth.dto.LoginRequest;
import com.schoolerp.modules.auth.dto.LoginResponse;
import com.schoolerp.modules.auth.entity.User;
import com.schoolerp.modules.auth.repository.UserRepository;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 5;

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SchoolLookupService schoolLookupService;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository
                .findByUsernameIgnoreCase(request.getUsername().trim())
                .orElseThrow(() -> new BusinessException("Invalid username or password"));
        UUID schoolId = user.getSchoolId();

        if (!user.isActive()) {
            throw new BusinessException("Your account has been deactivated. Please contact the administrator.");
        }

        if (user.isAccountLocked()) {
            throw new BusinessException("Account is temporarily locked due to too many failed attempts. Please try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new BusinessException("Invalid username or password");
        }

        // Successful login — reset failed attempts
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String schoolName = schoolLookupService.getSchoolName(schoolId);
        String accessToken = jwtUtil.generateAccessToken(user.getId(), schoolId, user.getRole(), user.getFullName());

        log.info("User '{}' logged in from school '{}'", user.getUsername(), schoolId);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .role(user.getRole())
                        .schoolId(schoolId)
                        .schoolName(schoolName)
                        .mustChangePassword(user.isMustChangePassword())
                        .build())
                .build();
    }

    public String refreshAccessToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new BusinessException("Invalid or expired refresh token");
        }
        Claims claims = jwtUtil.validateAndExtractClaims(refreshToken);
        if (!"REFRESH".equals(claims.get("type", String.class))) {
            throw new BusinessException("Invalid token type");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        UUID schoolId = UUID.fromString(claims.get("schoolId", String.class));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!user.isActive()) {
            throw new BusinessException("Account is deactivated");
        }

        return jwtUtil.generateAccessToken(user.getId(), schoolId, user.getRole(), user.getFullName());
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plusSeconds(LOCKOUT_MINUTES * 60L));
            log.warn("Account '{}' locked after {} failed attempts", user.getUsername(), attempts);
        }
        userRepository.save(user);
    }
}
