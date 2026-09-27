package com.schoolerp.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Removes sign-ups whose admin never signed in with the emailed credentials
 * (schools.email_verified_at is still NULL) after a grace period. Stops someone
 * from permanently squatting a school's email address (test&bug.md SEC-09).
 *
 * Only schools that have no students or payments are removed — an unverified
 * school can't have any, because signing in is what verifies it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnverifiedSchoolCleanup {

    public static final int GRACE_HOURS = 48;

    private final JdbcTemplate jdbcTemplate;

    /** If this email is held by a stale unverified sign-up, remove it so it can register again. */
    @Transactional
    public void releaseEmailIfStale(String email) {
        List<UUID> ids = jdbcTemplate.queryForList(
                staleSchoolsSql() + " AND LOWER(s.email) = ?", UUID.class, GRACE_HOURS, email);
        ids.forEach(this::purge);
    }

    @Scheduled(cron = "0 30 3 * * *") // daily, 03:30 server time
    @Transactional
    public void purgeStale() {
        List<UUID> ids = jdbcTemplate.queryForList(staleSchoolsSql(), UUID.class, GRACE_HOURS);
        ids.forEach(this::purge);
        if (!ids.isEmpty()) {
            log.info("Removed {} unverified school sign-up(s) older than {}h", ids.size(), GRACE_HOURS);
        }
    }

    private static String staleSchoolsSql() {
        return """
                SELECT s.id FROM schools s
                WHERE s.email_verified_at IS NULL
                  AND s.created_at < NOW() - make_interval(hours => ?)
                  AND NOT EXISTS (SELECT 1 FROM students st WHERE st.school_id = s.id)
                  AND NOT EXISTS (SELECT 1 FROM payment_transactions p WHERE p.school_id = s.id)
                """;
    }

    private void purge(UUID schoolId) {
        // Children first; user_otps and revoked_refresh_tokens cascade from users
        jdbcTemplate.update("DELETE FROM audit_logs WHERE school_id = ?", schoolId);
        jdbcTemplate.update("DELETE FROM monthly_fee_structures WHERE school_id = ?", schoolId);
        jdbcTemplate.update("DELETE FROM fee_categories WHERE school_id = ?", schoolId);
        jdbcTemplate.update("DELETE FROM sections WHERE school_id = ?", schoolId);
        jdbcTemplate.update("DELETE FROM classes WHERE school_id = ?", schoolId);
        jdbcTemplate.update("DELETE FROM academic_years WHERE school_id = ?", schoolId);
        jdbcTemplate.update("DELETE FROM users WHERE school_id = ?", schoolId);
        jdbcTemplate.update("DELETE FROM schools WHERE id = ?", schoolId);
        log.info("Purged unverified school {}", schoolId);
    }
}
