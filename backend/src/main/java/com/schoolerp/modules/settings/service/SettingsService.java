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
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

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

    public void changePassword(ChangePasswordRequest req, UUID userId) {
        String currentHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?", String.class, userId);
                
        if (!passwordEncoder.matches(req.getCurrentPassword(), currentHash)) {
            throw new BusinessException("Current password is incorrect");
        }
        
        if (req.getNewPassword().length() < 8) {
            throw new BusinessException("New password must be at least 8 characters");
        }
        if (req.getNewPassword().equals(req.getCurrentPassword())) {
            throw new BusinessException("New password must be different from the current password");
        }

        String newHash = passwordEncoder.encode(req.getNewPassword());
        jdbcTemplate.update(
                "UPDATE users SET password_hash = ?, must_change_password = FALSE, updated_at = NOW() WHERE id = ?",
                newHash, userId);
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
