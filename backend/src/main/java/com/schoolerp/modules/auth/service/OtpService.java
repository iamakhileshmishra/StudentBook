package com.schoolerp.modules.auth.service;

import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * 6-digit one-time codes sent by email. Each code is tied to a user and a
 * purpose, expires after 10 minutes, allows 5 wrong attempts, and only the
 * newest code per user+purpose is valid.
 *
 * Callers that run in a transaction must not roll back on BusinessException
 * (noRollbackFor), otherwise wrong attempts are not counted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    public enum Purpose {
        PASSWORD_RESET("reset the password", "Your School ERP password reset code"),
        PASSWORD_CHANGE("confirm the password change", "Confirm your School ERP password change");

        private final String action;
        private final String subject;

        Purpose(String action, String subject) {
            this.action = action;
            this.subject = subject;
        }
    }

    /** The account a code is issued to. */
    public record Recipient(UUID userId, String username, String fullName, String email) {}

    public static final int EXPIRY_MINUTES = 10;
    public static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_CODES_PER_DAY = 10;
    private static final String INVALID_CODE = "This code is invalid or has expired. Please request a new one.";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private record Otp(UUID id, String hash, int attempts) {}

    /**
     * Seconds until a new code may be sent, or 0 if one can be sent now.
     */
    public int cooldownRemaining(UUID userId, Purpose purpose) {
        Integer secs = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(CEIL(EXTRACT(EPOCH FROM
                       (created_at + make_interval(secs => ?) - NOW())))), 0)::int
                FROM user_otps WHERE user_id = ? AND purpose = ?
                  AND created_at > NOW() - make_interval(secs => ?)
                """, Integer.class, RESEND_COOLDOWN_SECONDS, userId, purpose.name(), RESEND_COOLDOWN_SECONDS);
        return secs == null ? 0 : Math.max(secs, 0);
    }

    /** Codes issued in the last 24 hours have reached the daily cap (SEC-08). */
    public boolean dailyLimitReached(UUID userId, Purpose purpose) {
        Integer n = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM user_otps
                WHERE user_id = ? AND purpose = ? AND created_at > NOW() - INTERVAL '24 hours'
                """, Integer.class, userId, purpose.name());
        return n != null && n >= MAX_CODES_PER_DAY;
    }

    /**
     * Generates, stores and emails a new code. Check cooldownRemaining() and
     * dailyLimitReached() first. With background=true the email is sent
     * asynchronously and failures are only logged, so the caller's response time
     * doesn't depend on SMTP (SEC-07).
     */
    public void issue(Recipient to, Purpose purpose, boolean background) {
        jdbcTemplate.update(
                "UPDATE user_otps SET used_at = NOW() WHERE user_id = ? AND purpose = ? AND used_at IS NULL",
                to.userId(), purpose.name());

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        jdbcTemplate.update("""
                INSERT INTO user_otps (user_id, purpose, otp_hash, expires_at)
                VALUES (?, ?, ?, NOW() + make_interval(mins => ?))
                """, to.userId(), purpose.name(), passwordEncoder.encode(code), EXPIRY_MINUTES);

        try {
            sendCodeEmail(to, purpose, code, background);
        } catch (MailException e) {
            log.error("Failed to send {} code for user {}", purpose, to.username(), e);
            throw new BusinessException("We couldn't send the email right now. Please try again in a few minutes.");
        }
        log.info("{} code sent for user {}", purpose, to.username());
    }

    /**
     * Checks the latest active code. Wrong codes count towards the attempt limit.
     * Returns the code's id so the caller can consume() it once the action succeeds.
     */
    public UUID verify(UUID userId, Purpose purpose, String code) {
        List<Otp> rows = jdbcTemplate.query("""
                SELECT id, otp_hash, attempts FROM user_otps
                WHERE user_id = ? AND purpose = ? AND used_at IS NULL AND expires_at > NOW()
                ORDER BY created_at DESC LIMIT 1
                """,
                (rs, i) -> new Otp((UUID) rs.getObject("id"), rs.getString("otp_hash"), rs.getInt("attempts")),
                userId, purpose.name());
        if (rows.isEmpty()) throw new BusinessException(INVALID_CODE);

        Otp otp = rows.get(0);
        if (otp.attempts() >= MAX_ATTEMPTS) {
            throw new BusinessException("Too many incorrect attempts. Please request a new code.");
        }
        if (!passwordEncoder.matches(code, otp.hash())) {
            jdbcTemplate.update("UPDATE user_otps SET attempts = attempts + 1 WHERE id = ?", otp.id());
            int left = MAX_ATTEMPTS - otp.attempts() - 1;
            throw new BusinessException(left > 0
                    ? "Incorrect code. " + left + (left == 1 ? " attempt" : " attempts") + " left."
                    : "Too many incorrect attempts. Please request a new code.");
        }
        return otp.id();
    }

    public void consume(UUID otpId) {
        jdbcTemplate.update("UPDATE user_otps SET used_at = NOW() WHERE id = ?", otpId);
    }

    /** Best-effort security notice after a password is changed or reset. */
    public void sendPasswordChangedNotice(Recipient to, String how) {
        String subject = "Your School ERP password was changed";
        String text = """
                Hello %s,

                The password for User ID %s was just %s.
                If this wasn't you, contact your school administrator or our support team immediately.
                """.formatted(to.fullName(), to.username(), how);
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;color:#0f172a">
                  <h2 style="color:#1a4fd6">Password changed</h2>
                  <p>Hello %s,</p>
                  <p>The password for User ID <strong>%s</strong> was just %s.</p>
                  <p style="color:#64748b;font-size:13px">If this wasn't you, contact your school administrator
                     or our support team immediately.</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(to.fullName()), HtmlUtils.htmlEscape(to.username()),
                HtmlUtils.htmlEscape(how));
        try {
            mailService.send(to.email(), subject, text, html);
        } catch (MailException e) {
            log.warn("Failed to send password-changed notice for user {}", to.username(), e);
        }
    }

    /** "admin@littleflower.test" → "ad***@littleflower.test" */
    public static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) return email;
        String local = email.substring(0, at);
        String visible = local.substring(0, Math.min(2, local.length()));
        return visible + "***" + email.substring(at);
    }

    private void sendCodeEmail(Recipient to, Purpose purpose, String code, boolean background) {
        String text = """
                Hello %s,

                Use this code to %s for User ID %s:

                %s

                The code expires in %d minutes. If you didn't request this, you can ignore
                this email — your password won't change.
                """.formatted(to.fullName(), purpose.action, to.username(), code, EXPIRY_MINUTES);
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;color:#0f172a">
                  <h2 style="color:#1a4fd6">Verification code</h2>
                  <p>Hello %s,</p>
                  <p>Use this code to %s for User ID <strong>%s</strong>:</p>
                  <p style="font-family:monospace;font-size:32px;letter-spacing:8px;font-weight:bold;
                            background:#f1f5f9;padding:16px;text-align:center;border-radius:8px">%s</p>
                  <p style="color:#64748b;font-size:13px">The code expires in %d minutes.</p>
                  <p style="color:#94a3b8;font-size:12px">If you didn't request this, you can ignore
                     this email — your password won't change.</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(to.fullName()), purpose.action,
                HtmlUtils.htmlEscape(to.username()), code, EXPIRY_MINUTES);
        if (background) {
            mailService.sendAsync(to.email(), purpose.subject, text, html);
        } else {
            mailService.send(to.email(), purpose.subject, text, html);
        }
    }
}
