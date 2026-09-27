package com.schoolerp.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Failed-password counter and temporary lockout.
 *
 * Each method commits in its own transaction (REQUIRES_NEW) so a failed attempt
 * is recorded even though the calling request then fails and rolls back
 * (test&bug.md SEC-03).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCKOUT_MINUTES = 5;

    private final JdbcTemplate jdbcTemplate;

    /** Records a wrong password. Returns true if the account is now locked. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailure(UUID userId) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT failed_attempts, locked_until FROM users WHERE id = ? FOR UPDATE", userId);
        int attempts = (Integer) row.get("failed_attempts");
        Timestamp lockedUntil = (Timestamp) row.get("locked_until");
        Instant now = Instant.now();

        // A lock that has run out starts a fresh count
        if (lockedUntil != null && !lockedUntil.toInstant().isAfter(now)) {
            attempts = 0;
        }
        attempts++;

        Timestamp newLock = null;
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            newLock = Timestamp.from(now.plusSeconds(LOCKOUT_MINUTES * 60L));
            log.warn("User {} locked for {} minutes after {} failed password attempts",
                    userId, LOCKOUT_MINUTES, attempts);
        }
        jdbcTemplate.update("UPDATE users SET failed_attempts = ?, locked_until = ? WHERE id = ?",
                attempts, newLock, userId);
        return newLock != null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID userId) {
        jdbcTemplate.update(
                "UPDATE users SET failed_attempts = 0, locked_until = NULL WHERE id = ? AND (failed_attempts <> 0 OR locked_until IS NOT NULL)",
                userId);
    }
}
