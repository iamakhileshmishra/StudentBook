package com.schoolerp.modules.auth.service;

import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Forgot password: email a 6-digit one-time code, verify it, then set a new password.
 *
 * Methods don't roll back on BusinessException so that wrong-code attempts are
 * still counted when the request itself fails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final String INVALID_CODE = "This code is invalid or has expired. Please request a new one.";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private record Account(UUID id, String username, String fullName) {}

    private record Otp(UUID id, String hash, int attempts) {}

    // ── Step 1: send code ───────────────────────────────────────────────────

    /**
     * Always completes silently for unknown emails so the endpoint can't be
     * used to discover which addresses are registered.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void sendResetCode(String rawEmail) {
        String email = normalize(rawEmail);
        Account account = findAccount(email);
        if (account == null) {
            log.info("Password reset requested for unregistered email");
            return;
        }

        Integer recent = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM password_reset_otps
                WHERE user_id = ? AND created_at > NOW() - make_interval(secs => ?)
                """, Integer.class, account.id(), RESEND_COOLDOWN_SECONDS);
        if (recent != null && recent > 0) {
            return; // a code was just sent; the UI enforces the same cooldown
        }

        // Only the newest code is ever valid
        jdbcTemplate.update(
                "UPDATE password_reset_otps SET used_at = NOW() WHERE user_id = ? AND used_at IS NULL",
                account.id());

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        jdbcTemplate.update("""
                INSERT INTO password_reset_otps (user_id, otp_hash, expires_at)
                VALUES (?, ?, NOW() + make_interval(mins => ?))
                """, account.id(), passwordEncoder.encode(code), OTP_EXPIRY_MINUTES);

        try {
            sendCodeEmail(email, account, code);
        } catch (MailException e) {
            log.error("Failed to send password reset code for user {}", account.username(), e);
            throw new BusinessException("We couldn't send the email right now. Please try again in a few minutes.");
        }
        log.info("Password reset code sent for user {}", account.username());
    }

    // ── Step 2: verify code ─────────────────────────────────────────────────

    @Transactional(noRollbackFor = BusinessException.class)
    public void verifyCode(String rawEmail, String code) {
        Account account = findAccount(normalize(rawEmail));
        if (account == null) throw new BusinessException(INVALID_CODE);
        checkCode(account, code);
    }

    // ── Step 3: reset password ──────────────────────────────────────────────

    @Transactional(noRollbackFor = BusinessException.class)
    public void resetPassword(String rawEmail, String code, String newPassword) {
        String email = normalize(rawEmail);
        Account account = findAccount(email);
        if (account == null) throw new BusinessException(INVALID_CODE);
        Otp otp = checkCode(account, code);

        // Also clears the first-login flag and any lockout: they've proven access to the email
        jdbcTemplate.update("""
                UPDATE users
                SET password_hash = ?, must_change_password = FALSE,
                    failed_attempts = 0, locked_until = NULL, updated_at = NOW()
                WHERE id = ?
                """, passwordEncoder.encode(newPassword), account.id());
        jdbcTemplate.update("UPDATE password_reset_otps SET used_at = NOW() WHERE id = ?", otp.id());

        log.info("Password reset completed for user {}", account.username());

        try {
            sendChangedEmail(email, account);
        } catch (MailException e) {
            // The reset already succeeded; the notice is informational only
            log.warn("Failed to send password-changed notice for user {}", account.username(), e);
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Returns the latest active code if it matches, otherwise counts the attempt and throws. */
    private Otp checkCode(Account account, String code) {
        List<Otp> rows = jdbcTemplate.query("""
                SELECT id, otp_hash, attempts FROM password_reset_otps
                WHERE user_id = ? AND used_at IS NULL AND expires_at > NOW()
                ORDER BY created_at DESC LIMIT 1
                """,
                (rs, i) -> new Otp((UUID) rs.getObject("id"), rs.getString("otp_hash"), rs.getInt("attempts")),
                account.id());
        if (rows.isEmpty()) throw new BusinessException(INVALID_CODE);

        Otp otp = rows.get(0);
        if (otp.attempts() >= MAX_ATTEMPTS) {
            throw new BusinessException("Too many incorrect attempts. Please request a new code.");
        }
        if (!passwordEncoder.matches(code, otp.hash())) {
            jdbcTemplate.update("UPDATE password_reset_otps SET attempts = attempts + 1 WHERE id = ?", otp.id());
            int left = MAX_ATTEMPTS - otp.attempts() - 1;
            throw new BusinessException(left > 0
                    ? "Incorrect code. " + left + (left == 1 ? " attempt" : " attempts") + " left."
                    : "Too many incorrect attempts. Please request a new code.");
        }
        return otp;
    }

    /**
     * Matches the user's own email first; falls back to the school's contact
     * email for that school's admin (e.g. seeded admins with no personal email).
     * Returns null unless exactly one active account matches.
     */
    private Account findAccount(String email) {
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
        return new Account((UUID) u.get("id"), (String) u.get("username"), (String) u.get("full_name"));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void sendCodeEmail(String to, Account account, String code) {
        String subject = "Your School ERP password reset code";
        String text = """
                Hello %s,

                Use this code to reset the password for User ID %s:

                %s

                The code expires in %d minutes. If you didn't ask to reset your password,
                you can ignore this email — your password won't change.
                """.formatted(account.fullName(), account.username(), code, OTP_EXPIRY_MINUTES);
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;color:#0f172a">
                  <h2 style="color:#1a4fd6">Reset your password</h2>
                  <p>Hello %s,</p>
                  <p>Use this code to reset the password for User ID <strong>%s</strong>:</p>
                  <p style="font-family:monospace;font-size:32px;letter-spacing:8px;font-weight:bold;
                            background:#f1f5f9;padding:16px;text-align:center;border-radius:8px">%s</p>
                  <p style="color:#64748b;font-size:13px">The code expires in %d minutes.</p>
                  <p style="color:#94a3b8;font-size:12px">If you didn't ask to reset your password, you can ignore
                     this email — your password won't change.</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(account.fullName()), HtmlUtils.htmlEscape(account.username()),
                code, OTP_EXPIRY_MINUTES);
        mailService.send(to, subject, text, html);
    }

    private void sendChangedEmail(String to, Account account) {
        String subject = "Your School ERP password was changed";
        String text = """
                Hello %s,

                The password for User ID %s was just reset using a code sent to this email.
                If this wasn't you, contact your school administrator or our support team immediately.
                """.formatted(account.fullName(), account.username());
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;color:#0f172a">
                  <h2 style="color:#1a4fd6">Password changed</h2>
                  <p>Hello %s,</p>
                  <p>The password for User ID <strong>%s</strong> was just reset using a code sent to this email.</p>
                  <p style="color:#64748b;font-size:13px">If this wasn't you, contact your school administrator
                     or our support team immediately.</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(account.fullName()), HtmlUtils.htmlEscape(account.username()));
        mailService.send(to, subject, text, html);
    }
}
