package com.schoolerp.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

/** The httpOnly refresh-token cookie (test&bug.md SEC-14: Secure + SameSite). */
@Component
public class RefreshCookieService {

    private static final String NAME = "refreshToken";
    private static final String PATH = "/api/auth";

    private final boolean secure;
    private final long maxAgeSeconds;

    public RefreshCookieService(@Value("${app.cookie.secure:false}") boolean secure, JwtUtil jwtUtil) {
        this.secure = secure;
        this.maxAgeSeconds = jwtUtil.getRefreshTokenExpirySeconds();
    }

    public void set(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(refreshToken, maxAgeSeconds).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", 0).toString());
    }

    public String read(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }

    private ResponseCookie build(String value, long maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(Duration.ofSeconds(maxAge))
                .build();
    }
}
