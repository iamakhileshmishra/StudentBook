package com.schoolerp.modules.auth;

import com.schoolerp.modules.auth.dto.ForgotPasswordRequest;
import com.schoolerp.modules.auth.dto.LoginRequest;
import com.schoolerp.modules.auth.dto.ResetPasswordRequest;
import com.schoolerp.modules.auth.dto.VerifyOtpRequest;
import com.schoolerp.modules.auth.service.PasswordResetService;
import com.schoolerp.modules.auth.dto.LoginResponse;
import com.schoolerp.modules.auth.dto.RegisterSchoolRequest;
import com.schoolerp.modules.auth.dto.RegisterSchoolResponse;
import com.schoolerp.modules.auth.service.AuthService;
import com.schoolerp.modules.auth.service.SchoolRegistrationService;
import com.schoolerp.shared.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SchoolRegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final JwtUtil jwtUtil;

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

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String ipAddress = httpRequest.getRemoteAddr();

        LoginResponse loginResponse = authService.login(request, ipAddress);

        // Generate and set refresh token as httpOnly cookie
        String refreshToken = jwtUtil.generateRefreshToken(
                loginResponse.getUser().getId(),
                loginResponse.getUser().getSchoolId()
        );
        setRefreshTokenCookie(httpResponse, refreshToken);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No refresh token"));
        }

        String newAccessToken = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    // ── Cookie helpers ──────────────────────────────────────────────────────

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production (HTTPS)
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) jwtUtil.getRefreshTokenExpirySeconds());
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
