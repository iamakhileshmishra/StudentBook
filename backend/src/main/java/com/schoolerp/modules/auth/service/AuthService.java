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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password";
    private static final String SESSION_EXPIRED = "Your session has expired. Please sign in again.";

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGuard passwordGuard;
    private final SchoolLookupService schoolLookupService;
    private final JdbcTemplate jdbcTemplate;

    /** Hash of a random value, compared against for unknown usernames so timing matches (SEC-07). */
    private volatile String dummyHash;

    /** Response body plus the refresh token the controller puts in the cookie. */
    public record AuthResult(LoginResponse body, String refreshToken) {}

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResult login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByUsernameIgnoreCase(request.getUsername().trim()).orElse(null);
        if (user == null) {
            // Same BCrypt cost as a real check so response time doesn't reveal unknown usernames
            passwordEncoder.matches(request.getPassword(), dummyHash());
            throw new BusinessException(INVALID_CREDENTIALS);
        }

        // Counts wrong passwords and locks after 5 (committed even though we then throw)
        passwordGuard.verify(user, request.getPassword(), INVALID_CREDENTIALS);

        // Only revealed to someone who knows the password
        if (!user.isActive()) {
            throw new BusinessException("Your account has been deactivated. Please contact the administrator.");
        }
        if (!schoolLookupService.isActive(user.getSchoolId())) {
            throw new BusinessException("Your school's account is inactive. Please contact support.");
        }

        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Signing in with the emailed temporary password proves the school owns its email (SEC-09)
        schoolLookupService.markEmailVerified(user.getSchoolId());

        log.info("User '{}' logged in from school '{}' ({})", user.getUsername(), user.getSchoolId(), ipAddress);
        return issueTokens(user);
    }

    /** Access + refresh tokens and the user payload for a fresh session. */
    public AuthResult issueTokens(User user) {
        UUID schoolId = user.getSchoolId();
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), schoolId, user.getRole(), user.getFullName(), user.getTokenVersion());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), schoolId, user.getTokenVersion());

        LoginResponse body = LoginResponse.builder()
                .accessToken(accessToken)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .role(user.getRole())
                        .schoolId(schoolId)
                        .schoolName(schoolLookupService.getSchoolName(schoolId))
                        .mustChangePassword(user.isMustChangePassword())
                        .build())
                .build();
        return new AuthResult(body, refreshToken);
    }

    /**
     * Exchanges a valid refresh token for a new session. Rejects revoked tokens (logout),
     * tokens from before a password change/reset, and deactivated users or schools.
     */
    @Transactional(readOnly = true)
    public AuthResult refresh(String refreshToken) {
        Claims claims = jwtUtil.parseOrNull(refreshToken);
        if (claims == null || !"REFRESH".equals(claims.get("type", String.class))) {
            throw new BusinessException(SESSION_EXPIRED);
        }
        if (claims.getId() != null && isRevoked(claims.getId())) {
            throw new BusinessException(SESSION_EXPIRED);
        }

        User user = userRepository.findById(UUID.fromString(claims.getSubject()))
                .orElseThrow(() -> new BusinessException(SESSION_EXPIRED));
        if (!user.isActive() || !schoolLookupService.isActive(user.getSchoolId())
                || JwtUtil.tokenVersion(claims) != user.getTokenVersion()) {
            throw new BusinessException(SESSION_EXPIRED);
        }
        return issueTokens(user);
    }

    /** Revokes this refresh token so it can't be replayed after logout (SEC-04). */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null) return;
        Claims claims = jwtUtil.parseOrNull(refreshToken);
        if (claims == null || claims.getId() == null) return;

        jdbcTemplate.update("DELETE FROM revoked_refresh_tokens WHERE expires_at < NOW()");
        jdbcTemplate.update("""
                INSERT INTO revoked_refresh_tokens (jti, user_id, expires_at) VALUES (?, ?, ?)
                ON CONFLICT (jti) DO NOTHING
                """,
                UUID.fromString(claims.getId()), UUID.fromString(claims.getSubject()),
                Timestamp.from(claims.getExpiration().toInstant()));
    }

    private boolean isRevoked(String jti) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM revoked_refresh_tokens WHERE jti = ?", Integer.class, UUID.fromString(jti));
        return n != null && n > 0;
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
        }
        return dummyHash;
    }
}
