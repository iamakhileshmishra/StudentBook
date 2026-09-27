package com.schoolerp.modules.auth.service;

import com.schoolerp.modules.auth.service.OtpService.Purpose;
import com.schoolerp.modules.auth.service.OtpService.Recipient;
import com.schoolerp.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Forgot password: email a one-time code, verify it, then set a new password.
 *
 * Methods don't roll back on BusinessException so that wrong-code attempts are
 * still counted when the request itself fails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String INVALID_CODE = "This code is invalid or has expired. Please request a new one.";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    // ── Step 1: send code ───────────────────────────────────────────────────

    /**
     * Always completes silently for unknown emails (and during the resend
     * cooldown) so the endpoint can't be used to discover registered addresses.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void sendResetCode(String rawEmail) {
        Recipient account = findAccount(normalize(rawEmail));
        if (account == null) {
            log.info("Password reset requested for unregistered email");
            return;
        }
        if (otpService.cooldownRemaining(account.userId(), Purpose.PASSWORD_RESET) > 0) {
            return; // a code was just sent; the UI enforces the same cooldown
        }
        otpService.issue(account, Purpose.PASSWORD_RESET);
    }

    // ── Step 2: verify code ─────────────────────────────────────────────────

    @Transactional(noRollbackFor = BusinessException.class)
    public void verifyCode(String rawEmail, String code) {
        Recipient account = findAccount(normalize(rawEmail));
        if (account == null) throw new BusinessException(INVALID_CODE);
        otpService.verify(account.userId(), Purpose.PASSWORD_RESET, code);
    }

    // ── Step 3: reset password ──────────────────────────────────────────────

    @Transactional(noRollbackFor = BusinessException.class)
    public void resetPassword(String rawEmail, String code, String newPassword) {
        Recipient account = findAccount(normalize(rawEmail));
        if (account == null) throw new BusinessException(INVALID_CODE);
        UUID otpId = otpService.verify(account.userId(), Purpose.PASSWORD_RESET, code);

        // Also clears the first-login flag and any lockout: they've proven access to the email
        jdbcTemplate.update("""
                UPDATE users
                SET password_hash = ?, must_change_password = FALSE,
                    failed_attempts = 0, locked_until = NULL, updated_at = NOW()
                WHERE id = ?
                """, passwordEncoder.encode(newPassword), account.userId());
        otpService.consume(otpId);

        log.info("Password reset completed for user {}", account.username());
        otpService.sendPasswordChangedNotice(account, "reset using a code sent to this email");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Matches the user's own email first; falls back to the school's contact
     * email for that school's admin (e.g. seeded admins with no personal email).
     * Returns null unless exactly one active account matches.
     */
    private Recipient findAccount(String email) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, username, full_name FROM users WHERE LOWER(email) = ? AND is_active = TRUE", email);
        if (users.isEmpty()) {
            users = jdbcTemplate.queryForList("""
                    SELECT u.id, u.username, u.full_name
                    FROM users u JOIN schools s ON s.id = u.school_id
                    WHERE LOWER(s.email) = ? AND u.role = 'SCHOOL_ADMIN' AND u.is_active = TRUE
                    """, email);
        }
        if (users.size() != 1) {
            if (users.size() > 1) log.warn("Password reset email matches {} accounts; ignoring", users.size());
            return null;
        }
        Map<String, Object> u = users.get(0);
        return new Recipient((UUID) u.get("id"), (String) u.get("username"), (String) u.get("full_name"), email);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
