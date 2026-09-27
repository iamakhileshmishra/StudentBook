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

    private record PasswordAccount(String passwordHash, boolean mustChangePassword, Recipient recipient) {}

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
        otpService.issue(account.recipient(), Purpose.PASSWORD_CHANGE);
        return Map.of(
                "sentTo", OtpService.maskEmail(account.recipient().email()),
                "resendAfterSeconds", OtpService.RESEND_COOLDOWN_SECONDS);
    }

    /**
     * Step 2: change the password. Requires the emailed code, except on first
     * login when the user is replacing the temporary password they were emailed.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void changePassword(ChangePasswordRequest req, UUID userId) {
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
        jdbcTemplate.update(
                "UPDATE users SET password_hash = ?, must_change_password = FALSE, updated_at = NOW() WHERE id = ?",
                newHash, userId);

        if (otpId != null) {
            otpService.consume(otpId);
            otpService.sendPasswordChangedNotice(account.recipient(), "changed from Settings");
        }
    }

    private PasswordAccount loadPasswordAccount(UUID userId) {
        // Admins seeded without a personal email fall back to the school's email
        return jdbcTemplate.queryForObject("""
                SELECT u.password_hash, u.must_change_password, u.username, u.full_name,
                       COALESCE(u.email, s.email) AS email
                FROM users u JOIN schools s ON s.id = u.school_id
                WHERE u.id = ?
                """,
                (rs, i) -> new PasswordAccount(
                        rs.getString("password_hash"),
                        rs.getBoolean("must_change_password"),
                        new Recipient(userId, rs.getString("username"), rs.getString("full_name"), rs.getString("email"))),
                userId);
    }

    private void validatePasswordChange(ChangePasswordRequest req, PasswordAccount account) {
        if (!passwordEncoder.matches(req.getCurrentPassword(), account.passwordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
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
        jdbcTemplate.update(
                "UPDATE academic_years SET start_date = ?, end_date = ? WHERE id = ? AND school_id = ?",
                req.getStartDate(), req.getEndDate(), req.getId(), schoolId
        );
        return getCurrentAcademicYear();
    }
}
