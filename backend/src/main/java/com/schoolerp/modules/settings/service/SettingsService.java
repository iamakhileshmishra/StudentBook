package com.schoolerp.modules.settings.service;

import com.schoolerp.modules.settings.dto.ChangePasswordRequest;
import com.schoolerp.modules.settings.dto.SchoolProfileDto;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolerp.modules.settings.dto.LateFeeRuleDto;
import com.schoolerp.modules.settings.dto.AcademicYearDto;
import com.schoolerp.modules.auth.entity.User;
import com.schoolerp.modules.auth.repository.UserRepository;
import com.schoolerp.modules.auth.service.AuthService;
import com.schoolerp.modules.auth.service.AuthService.AuthResult;
import com.schoolerp.modules.auth.service.PasswordGuard;
import jakarta.persistence.EntityManager;
import com.schoolerp.modules.auth.service.OtpService;
import com.schoolerp.modules.auth.service.OtpService.Purpose;
import com.schoolerp.modules.auth.service.OtpService.Recipient;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final OtpService otpService;
    private final PasswordGuard passwordGuard;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final EntityManager entityManager;

    public SchoolProfileDto getSchoolProfile() {
        UUID schoolId = TenantContext.getSchoolId();
        
        return jdbcTemplate.queryForObject(
                "SELECT name, short_name, address_line1, address_line2, city, state, pincode, phone, email, website, logo_url, fee_due_date, late_fee_rules FROM schools WHERE id = ?",
                (rs, rowNum) -> {
                    List<LateFeeRuleDto> rules = new ArrayList<>();
                    String rulesJson = rs.getString("late_fee_rules");
                    if (rulesJson != null && !rulesJson.isEmpty()) {
                        try {
                            rules = objectMapper.readValue(rulesJson, new TypeReference<List<LateFeeRuleDto>>() {});
                        } catch (JsonProcessingException e) {
                            // ignore, return empty list
                        }
                    }
                    return SchoolProfileDto.builder()
                        .name(rs.getString("name"))
                        .shortName(rs.getString("short_name"))
                        .addressLine1(rs.getString("address_line1"))
                        .addressLine2(rs.getString("address_line2"))
                        .city(rs.getString("city"))
                        .state(rs.getString("state"))
                        .pincode(rs.getString("pincode"))
                        .phone(rs.getString("phone"))
                        .email(rs.getString("email"))
                        .website(rs.getString("website"))
                        .logoUrl(rs.getString("logo_url"))
                        .feeDueDate(rs.getObject("fee_due_date") != null ? rs.getInt("fee_due_date") : null)
                        .lateFeeRules(rules)
                        .build();
                },
                schoolId
        );
    }

    public SchoolProfileDto updateSchoolProfile(SchoolProfileDto req) {
        UUID schoolId = TenantContext.getSchoolId();
        validateLateFeeRules(req.getLateFeeRules());

        // Friendly error instead of a unique-index 500 when another school uses the email (BUG-16)
        String email = req.getEmail().trim().toLowerCase();
        Integer taken = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schools WHERE LOWER(email) = ? AND id <> ?", Integer.class, email, schoolId);
        if (taken != null && taken > 0) {
            throw new BusinessException("Another school is already registered with this email address.");
        }
        req.setEmail(email);
        req.setShortName(req.getShortName() == null || req.getShortName().isBlank()
                ? null : req.getShortName().trim().toUpperCase());
        
        String rulesJson = "[]";
        try {
            if (req.getLateFeeRules() != null) {
                rulesJson = objectMapper.writeValueAsString(req.getLateFeeRules());
            }
        } catch (JsonProcessingException e) {
            // ignore
        }
        
        jdbcTemplate.update("""
                UPDATE schools 
                SET name = ?, short_name = ?, address_line1 = ?, address_line2 = ?, 
                    city = ?, state = ?, pincode = ?, phone = ?, email = ?, 
                    website = ?, logo_url = ?, fee_due_date = ?, late_fee_rules = ?::jsonb, updated_at = NOW()
                WHERE id = ?
                """,
                req.getName(), req.getShortName(), req.getAddressLine1(), req.getAddressLine2(),
                req.getCity(), req.getState(), req.getPincode(), req.getPhone(), req.getEmail(),
                req.getWebsite(), req.getLogoUrl(), req.getFeeDueDate(), rulesJson, schoolId
        );
        
        return getSchoolProfile();
    }

    // ─── Change password (confirmed by an emailed code) ─────────────────────

    private record PasswordAccount(User user, boolean mustChangePassword, Recipient recipient) {}

    /**
     * Step 1: check the current password and new-password rules, then email a
     * verification code. Returns where the code was sent (masked).
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public Map<String, Object> requestPasswordChangeCode(ChangePasswordRequest req, UUID userId) {
        PasswordAccount account = loadPasswordAccount(userId);
        validatePasswordChange(req, account);
        requireEmail(account);

        int wait = otpService.cooldownRemaining(userId, Purpose.PASSWORD_CHANGE);
        if (wait > 0) {
            throw new BusinessException("Please wait " + wait + " seconds before requesting a new code.");
        }
        if (otpService.dailyLimitReached(userId, Purpose.PASSWORD_CHANGE)) {
            throw new BusinessException("Too many verification codes requested today. Please try again tomorrow.");
        }
        otpService.issue(account.recipient(), Purpose.PASSWORD_CHANGE, false);
        return Map.of(
                "sentTo", OtpService.maskEmail(account.recipient().email()),
                "resendAfterSeconds", OtpService.RESEND_COOLDOWN_SECONDS);
    }

    /**
     * Step 2: change the password. Requires the emailed code, except on first
     * login when the user is replacing the temporary password they were emailed.
     *
     * Bumps token_version so every other session is signed out (SEC-04), and
     * returns fresh tokens so the current session carries on.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResult changePassword(ChangePasswordRequest req, UUID userId) {
        PasswordAccount account = loadPasswordAccount(userId);
        validatePasswordChange(req, account);

        UUID otpId = null;
        if (!account.mustChangePassword()) {
            if (req.getOtp() == null || req.getOtp().isBlank()) {
                throw new BusinessException("Enter the verification code sent to your email.");
            }
            otpId = otpService.verify(userId, Purpose.PASSWORD_CHANGE, req.getOtp());
        }

        String newHash = passwordEncoder.encode(req.getNewPassword());
        jdbcTemplate.update("""
                UPDATE users SET password_hash = ?, must_change_password = FALSE,
                                 token_version = token_version + 1, updated_at = NOW()
                WHERE id = ?
                """, newHash, userId);

        if (otpId != null) {
            otpService.consume(otpId);
            otpService.sendPasswordChangedNotice(account.recipient(), "changed from Settings");
        }

        User updated = userRepository.findById(userId).orElseThrow();
        entityManager.refresh(updated); // pick up the JDBC update above
        return authService.issueTokens(updated);
    }

    private PasswordAccount loadPasswordAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        // Admins seeded without a personal email fall back to the school's email
        String email = jdbcTemplate.queryForObject(
                "SELECT COALESCE(u.email, s.email) FROM users u JOIN schools s ON s.id = u.school_id WHERE u.id = ?",
                String.class, userId);
        return new PasswordAccount(user, user.isMustChangePassword(),
                new Recipient(userId, user.getUsername(), user.getFullName(), email));
    }

    private void validatePasswordChange(ChangePasswordRequest req, PasswordAccount account) {
        // Shares the login lockout, so a hijacked session can't brute-force the password (SEC-11)
        passwordGuard.verify(account.user(), req.getCurrentPassword(), "Current password is incorrect");
        if (req.getNewPassword().length() < 8) {
            throw new BusinessException("New password must be at least 8 characters");
        }
        if (req.getNewPassword().equals(req.getCurrentPassword())) {
            throw new BusinessException("New password must be different from the current password");
        }
    }

    private static void requireEmail(PasswordAccount account) {
        String email = account.recipient().email();
        if (email == null || email.isBlank()) {
            throw new BusinessException(
                    "No email address on file to send a verification code. Add one in School Profile first.");
        }
    }

    /** Rules must be well-formed and must not overlap (BUG-16). */
    private static void validateLateFeeRules(List<LateFeeRuleDto> rules) {
        if (rules == null || rules.isEmpty()) return;
        List<LateFeeRuleDto> sorted = new ArrayList<>(rules);
        sorted.sort(java.util.Comparator.comparingInt(LateFeeRuleDto::getStartDaysLate));
        for (int i = 0; i < sorted.size(); i++) {
            LateFeeRuleDto r = sorted.get(i);
            if (r.getEndDaysLate() < r.getStartDaysLate()) {
                throw new BusinessException("Each late fee rule must end on or after the day it starts.");
            }
            if (i > 0 && r.getStartDaysLate() <= sorted.get(i - 1).getEndDaysLate()) {
                throw new BusinessException("Late fee rules must not overlap.");
            }
        }
    }

    public AcademicYearDto getCurrentAcademicYear() {
        UUID schoolId = TenantContext.getSchoolId();
        return jdbcTemplate.queryForObject(
                "SELECT id, name, start_date, end_date, is_current FROM academic_years WHERE school_id = ? AND is_current = true",
                (rs, rowNum) -> {
                    AcademicYearDto dto = new AcademicYearDto();
                    dto.setId((UUID) rs.getObject("id"));
                    dto.setName(rs.getString("name"));
                    dto.setStartDate(rs.getDate("start_date").toLocalDate());
                    dto.setEndDate(rs.getDate("end_date").toLocalDate());
                    dto.setIsCurrent(rs.getBoolean("is_current"));
                    return dto;
                },
                schoolId
        );
    }

    public AcademicYearDto updateCurrentAcademicYear(AcademicYearDto req) {
        UUID schoolId = TenantContext.getSchoolId();
        // BUG-16: validate before the DB check constraint turns it into a 500
        if (req.getId() == null || req.getStartDate() == null || req.getEndDate() == null) {
            throw new BusinessException("Start and end dates are required.");
        }
        if (!req.getEndDate().isAfter(req.getStartDate())) {
            throw new BusinessException("The academic year must end after it starts.");
        }
        if (req.getStartDate().plusMonths(18).isBefore(req.getEndDate())) {
            throw new BusinessException("An academic year can be at most 18 months long.");
        }
        Integer overlaps = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_years
                WHERE school_id = ? AND id <> ? AND start_date <= ? AND end_date >= ?
                """, Integer.class, schoolId, req.getId(), req.getEndDate(), req.getStartDate());
        if (overlaps != null && overlaps > 0) {
            throw new BusinessException("These dates overlap another academic year.");
        }
        jdbcTemplate.update(
                "UPDATE academic_years SET start_date = ?, end_date = ? WHERE id = ? AND school_id = ?",
                req.getStartDate(), req.getEndDate(), req.getId(), schoolId
        );
        return getCurrentAcademicYear();
    }
}
