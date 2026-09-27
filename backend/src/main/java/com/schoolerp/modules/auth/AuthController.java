package com.schoolerp.modules.auth;

import com.schoolerp.modules.auth.dto.ForgotPasswordRequest;
import com.schoolerp.modules.auth.dto.LoginRequest;
import com.schoolerp.modules.auth.dto.LoginResponse;
import com.schoolerp.modules.auth.dto.RegisterSchoolRequest;
import com.schoolerp.modules.auth.dto.RegisterSchoolResponse;
import com.schoolerp.modules.auth.dto.ResetPasswordRequest;
import com.schoolerp.modules.auth.dto.VerifyOtpRequest;
import com.schoolerp.modules.auth.service.AuthService;
import com.schoolerp.modules.auth.service.AuthService.AuthResult;
import com.schoolerp.modules.auth.service.PasswordResetService;
import com.schoolerp.modules.auth.service.SchoolRegistrationService;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.security.RefreshCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SchoolRegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final RefreshCookieService refreshCookie;

    @PostMapping("/register")
    public ResponseEntity<RegisterSchoolResponse> register(@Valid @RequestBody RegisterSchoolRequest request) {
        return ResponseEntity.ok(registrationService.register(request));
    }

    // ── Forgot password (email + OTP) ───────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendResetCode(request.getEmail());
        // Same response whether or not the email is registered
        return ResponseEntity.ok(Map.of("message",
                "If an account is registered with this email, a verification code has been sent."));
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<Void> verifyResetCode(@Valid @RequestBody VerifyOtpRequest request) {
        passwordResetService.verifyCode(request.getEmail(), request.getOtp());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    // ── Session ─────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResult result = authService.login(request, httpRequest.getRemoteAddr());
        refreshCookie.set(httpResponse, result.refreshToken());
        return ResponseEntity.ok(result.body());
    }

    /**
     * New access token (and the current user) from the refresh cookie. Also used on
     * page load to restore the session. 401 + cleared cookie if the session is gone.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshCookie.read(request);
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No refresh token"));
        }
        try {
            return ResponseEntity.ok(authService.refresh(refreshToken).body());
        } catch (BusinessException e) {
            refreshCookie.clear(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(refreshCookie.read(request));
        refreshCookie.clear(response);
        return ResponseEntity.noContent().build();
    }
}
