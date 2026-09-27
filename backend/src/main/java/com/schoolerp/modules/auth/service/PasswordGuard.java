package com.schoolerp.modules.auth.service;

import com.schoolerp.modules.auth.entity.User;
import com.schoolerp.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Single place that checks a user's password, shared by login and by the
 * in-app actions that re-ask for it (change password, remove student).
 * Wrong guesses count towards the same lockout everywhere (SEC-03, SEC-11).
 */
@Component
@RequiredArgsConstructor
public class PasswordGuard {

    public static final String LOCKED_MESSAGE =
            "Too many incorrect password attempts. Please try again in " + LoginAttemptService.LOCKOUT_MINUTES + " minutes.";

    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    /**
     * Throws BusinessException(wrongPasswordMessage) on a wrong password, or the
     * lock message if the account is (or just became) locked.
     */
    public void verify(User user, String rawPassword, String wrongPasswordMessage) {
        if (user.isAccountLocked()) {
            throw new BusinessException(LOCKED_MESSAGE);
        }
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            boolean nowLocked = loginAttemptService.recordFailure(user.getId());
            throw new BusinessException(nowLocked ? LOCKED_MESSAGE : wrongPasswordMessage);
        }
        loginAttemptService.recordSuccess(user.getId());
    }
}
