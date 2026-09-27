package com.schoolerp.modules.auth.service;

import com.schoolerp.modules.auth.dto.RegisterSchoolRequest;
import com.schoolerp.modules.auth.dto.RegisterSchoolResponse;
import com.schoolerp.modules.auth.entity.User;
import com.schoolerp.modules.auth.repository.UserRepository;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Self-service onboarding: creates a school, its first admin (with a
 * temporary password), the current academic year and default fee
 * categories, then emails the login details to the owner.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolRegistrationService {

    private static final List<String> DEFAULT_FEE_CATEGORIES = List.of(
            "Tuition Fee", "Exam Fee", "Development Fee", "Computer Fee",
            "Transport Fee", "Annual Charges", "Miscellaneous");

    private static final String USERNAME_SUFFIX = ".admin";
    private static final int MAX_USERNAME_BASE = 12;
    private static final int DEFAULT_FEE_DUE_DATE = 10;

    // No look-alike characters (0/O, 1/l/I) so the emailed password is easy to type
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final UnverifiedSchoolCleanup unverifiedSchoolCleanup;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public RegisterSchoolResponse register(RegisterSchoolRequest req) {
        String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        String schoolName = req.getSchoolName().trim();
        // Default the receipt prefix to the school's initials so receipts and the User ID match
        String shortName = blankToNull(req.getShortName());
        if (shortName == null) {
            String initials = initials(schoolName).toUpperCase(Locale.ROOT);
            shortName = initials.length() >= 2 ? initials.substring(0, Math.min(10, initials.length())) : null;
        }

        // An old sign-up that was never activated doesn't block the address forever (SEC-09)
        unverifiedSchoolCleanup.releaseEmailIfStale(email);

        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schools WHERE LOWER(email) = ?", Integer.class, email);
        if (existing != null && existing > 0) {
            throw new BusinessException("A school is already registered with this email address. "
                    + "If you registered recently, check your inbox for the login details.");
        }

        UUID schoolId = jdbcTemplate.queryForObject("""
                INSERT INTO schools (name, short_name, address_line1, city, state, pincode,
                                     phone, email, fee_due_date, is_active, subscription_plan)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, 'BASIC')
                RETURNING id
                """, UUID.class,
                schoolName, shortName == null ? null : shortName.toUpperCase(Locale.ROOT),
                req.getAddressLine1().trim(), req.getCity().trim(), req.getState().trim(),
                req.getPincode().trim(), req.getPhone().trim(), email,
                req.getFeeDueDate() != null ? req.getFeeDueDate() : DEFAULT_FEE_DUE_DATE);

        String username = generateUsername(schoolName, shortName);
        String tempPassword = generateTempPassword();

        User admin = User.builder()
                .schoolId(schoolId)
                .username(username)
                .email(email)
                .fullName(req.getOwnerName().trim())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role("SCHOOL_ADMIN")
                .active(true)
                .mustChangePassword(true)
                .build();
        userRepository.save(admin);

        createCurrentAcademicYear(schoolId);
        createDefaultFeeCategories(schoolId);

        // Sent inside the transaction: if the email can't be delivered the whole
        // registration rolls back, so the owner is never left without credentials.
        try {
            sendWelcomeEmail(email, req.getOwnerName().trim(), schoolName, username, tempPassword);
        } catch (MailException e) {
            log.error("Failed to send registration email to {}", email, e);
            throw new BusinessException(
                    "We couldn't send the confirmation email. Please check the address and try again.");
        }

        log.info("Registered school '{}' ({}) with admin '{}'", schoolName, schoolId, username);

        return RegisterSchoolResponse.builder()
                .schoolName(schoolName)
                .email(email)
                .build();
    }

    // ── Username ────────────────────────────────────────────────────────────

    /**
     * Builds a platform-unique, permanent username from the school's short name,
     * or the initials of its name: "New Shanti Niketan Vidyapeeth Public School"
     * → "nsnvps.admin", then "nsnvps2.admin" if that is taken.
     */
    String generateUsername(String schoolName, String shortName) {
        String base = shortName != null ? shortName : initials(schoolName);
        base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (base.length() < 3) {
            // Very short names ("DPS" is fine, "St X" isn't): fall back to the first word
            String firstWord = schoolName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", "").trim().split("\\s+")[0];
            if (firstWord.length() > base.length()) base = firstWord;
        }
        if (base.isEmpty()) base = "school";
        if (base.length() > MAX_USERNAME_BASE) base = base.substring(0, MAX_USERNAME_BASE);

        String candidate = base + USERNAME_SUFFIX;
        for (int n = 2; userRepository.existsByUsernameIgnoreCase(candidate); n++) {
            candidate = base + n + USERNAME_SUFFIX;
        }
        return candidate;
    }

    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            String clean = word.replaceAll("[^A-Za-z0-9]", "");
            if (!clean.isEmpty()) sb.append(clean.charAt(0));
        }
        return sb.toString();
    }

    // ── Temporary password ──────────────────────────────────────────────────

    /** 10 characters with at least one upper, lower, digit and symbol. */
    static String generateTempPassword() {
        String all = UPPER + LOWER + DIGITS + SYMBOLS;
        List<Character> chars = new ArrayList<>();
        chars.add(randomChar(UPPER));
        chars.add(randomChar(LOWER));
        chars.add(randomChar(DIGITS));
        chars.add(randomChar(SYMBOLS));
        while (chars.size() < 10) chars.add(randomChar(all));
        Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder();
        chars.forEach(sb::append);
        return sb.toString();
    }

    private static char randomChar(String pool) {
        return pool.charAt(RANDOM.nextInt(pool.length()));
    }

    // ── Starter data ────────────────────────────────────────────────────────

    /** Indian academic year: April 1 – March 31. */
    private void createCurrentAcademicYear(UUID schoolId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        int startYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        String name = startYear + "-" + String.format("%02d", (startYear + 1) % 100);
        jdbcTemplate.update("""
                INSERT INTO academic_years (school_id, name, start_date, end_date, is_current)
                VALUES (?, ?, ?, ?, TRUE)
                """,
                schoolId, name, LocalDate.of(startYear, 4, 1), LocalDate.of(startYear + 1, 3, 31));
    }

    private void createDefaultFeeCategories(UUID schoolId) {
        for (int i = 0; i < DEFAULT_FEE_CATEGORIES.size(); i++) {
            jdbcTemplate.update(
                    "INSERT INTO fee_categories (school_id, name, display_order, is_active) VALUES (?, ?, ?, TRUE)",
                    schoolId, DEFAULT_FEE_CATEGORIES.get(i), i + 1);
        }
    }

    // ── Email ───────────────────────────────────────────────────────────────

    private void sendWelcomeEmail(String to, String ownerName, String schoolName,
                                  String username, String tempPassword) {
        String loginUrl = frontendUrl + "/login";
        String subject = "Your School ERP account for " + schoolName;

        String text = """
                Hello %s,

                Your School ERP account for %s has been created.

                User ID: %s
                Temporary password: %s

                Sign in at %s
                You will be asked to set a new password when you first sign in.
                Your User ID is permanent and cannot be changed.

                If you did not request this account, please ignore this email.
                """.formatted(ownerName, schoolName, username, tempPassword, loginUrl);

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;color:#0f172a">
                  <h2 style="color:#1a4fd6">Welcome to School ERP</h2>
                  <p>Hello %s,</p>
                  <p>Your account for <strong>%s</strong> has been created. Use these details to sign in:</p>
                  <table style="border-collapse:collapse;margin:16px 0">
                    <tr><td style="padding:6px 16px 6px 0;color:#64748b">User ID</td>
                        <td style="padding:6px 0;font-family:monospace;font-size:16px"><strong>%s</strong></td></tr>
                    <tr><td style="padding:6px 16px 6px 0;color:#64748b">Temporary password</td>
                        <td style="padding:6px 0;font-family:monospace;font-size:16px"><strong>%s</strong></td></tr>
                  </table>
                  <p><a href="%s" style="display:inline-block;background:#1a4fd6;color:#fff;padding:10px 20px;
                        border-radius:8px;text-decoration:none">Sign in</a></p>
                  <p style="color:#64748b;font-size:13px">You will be asked to set a new password when you first sign in.
                     Your User ID is permanent and cannot be changed.</p>
                  <p style="color:#94a3b8;font-size:12px">If you did not request this account, please ignore this email.</p>
                </div>
                """.formatted(
                HtmlUtils.htmlEscape(ownerName), HtmlUtils.htmlEscape(schoolName),
                HtmlUtils.htmlEscape(username), HtmlUtils.htmlEscape(tempPassword), loginUrl);

        mailService.send(to, subject, text, html);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
