package com.schoolerp.modules.settings.service;

import com.schoolerp.modules.settings.dto.ChangePasswordRequest;
import com.schoolerp.modules.settings.dto.SchoolProfileDto;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public SchoolProfileDto getSchoolProfile() {
        UUID schoolId = TenantContext.getSchoolId();
        
        return jdbcTemplate.queryForObject(
                "SELECT name, short_name, address_line1, address_line2, city, state, pincode, phone, email, website, logo_url FROM schools WHERE id = ?",
                (rs, rowNum) -> SchoolProfileDto.builder()
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
                        .build(),
                schoolId
        );
    }

    public SchoolProfileDto updateSchoolProfile(SchoolProfileDto req) {
        UUID schoolId = TenantContext.getSchoolId();
        
        jdbcTemplate.update("""
                UPDATE schools 
                SET name = ?, short_name = ?, address_line1 = ?, address_line2 = ?, 
                    city = ?, state = ?, pincode = ?, phone = ?, email = ?, 
                    website = ?, logo_url = ?, updated_at = NOW()
                WHERE id = ?
                """,
                req.getName(), req.getShortName(), req.getAddressLine1(), req.getAddressLine2(),
                req.getCity(), req.getState(), req.getPincode(), req.getPhone(), req.getEmail(),
                req.getWebsite(), req.getLogoUrl(), schoolId
        );
        
        return getSchoolProfile();
    }

    public void changePassword(ChangePasswordRequest req, UUID userId) {
        String currentHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?", String.class, userId);
                
        if (!passwordEncoder.matches(req.getCurrentPassword(), currentHash)) {
            throw new BusinessException("Current password is incorrect");
        }
        
        String newHash = passwordEncoder.encode(req.getNewPassword());
        jdbcTemplate.update("UPDATE users SET password_hash = ?, updated_at = NOW() WHERE id = ?", newHash, userId);
    }
}
