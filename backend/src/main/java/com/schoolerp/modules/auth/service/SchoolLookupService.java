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
}
