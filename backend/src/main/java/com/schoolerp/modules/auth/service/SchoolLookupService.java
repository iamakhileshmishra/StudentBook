package com.schoolerp.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolLookupService {

    private final JdbcTemplate jdbcTemplate;

    public String getSchoolName(UUID schoolId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM schools WHERE id = ?",
                    String.class,
                    schoolId
            );
        } catch (Exception e) {
            return "Unknown School";
        }
    }

    public boolean isActive(UUID schoolId) {
        Boolean active = jdbcTemplate.query(
                "SELECT is_active FROM schools WHERE id = ?",
                rs -> rs.next() ? rs.getBoolean(1) : Boolean.FALSE,
                schoolId);
        return Boolean.TRUE.equals(active);
    }

    /** First successful sign-in proves the owner received the emailed credentials. */
    public void markEmailVerified(UUID schoolId) {
        jdbcTemplate.update(
                "UPDATE schools SET email_verified_at = NOW() WHERE id = ? AND email_verified_at IS NULL", schoolId);
    }
}
