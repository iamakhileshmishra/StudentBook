package com.schoolerp.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP fixed-window rate limits on the public auth endpoints (test&bug.md SEC-08):
 * slows password guessing, mass sign-ups and email bombing.
 *
 * In-memory, so limits are per backend instance; move to Redis/Bucket4j if the
 * backend is ever scaled out. Behind a reverse proxy set app.rate-limit.trust-proxy
 * so the client address comes from X-Forwarded-For.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String path, int limit, long windowSeconds) {}

    private record Window(long startEpochSecond, int count) {}

    private final List<Rule> rules;
    private final boolean trustProxy;
    private final boolean enabled;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.trust-proxy:false}") boolean trustProxy,
            @Value("${app.rate-limit.login-per-minute:10}") int loginPerMinute,
            @Value("${app.rate-limit.register-per-hour:5}") int registerPerHour,
            @Value("${app.rate-limit.forgot-password-per-15min:5}") int forgotPer15Min,
            @Value("${app.rate-limit.otp-check-per-15min:20}") int otpCheckPer15Min,
            @Value("${app.rate-limit.refresh-per-minute:60}") int refreshPerMinute) {
        this.enabled = enabled;
        this.trustProxy = trustProxy;
        this.rules = List.of(
                new Rule("/api/auth/login", loginPerMinute, 60),
                new Rule("/api/auth/register", registerPerHour, 3600),
                new Rule("/api/auth/forgot-password", forgotPer15Min, 900),
                new Rule("/api/auth/forgot-password/verify", otpCheckPer15Min, 900),
                new Rule("/api/auth/forgot-password/reset", otpCheckPer15Min, 900),
                new Rule("/api/auth/refresh", refreshPerMinute, 60));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equalsIgnoreCase(request.getMethod()) || findRule(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = findRule(request.getRequestURI());
        String key = rule.path() + "|" + clientIp(request);
        long now = System.currentTimeMillis() / 1000;

        Window w = windows.compute(key, (k, old) ->
                old == null || now - old.startEpochSecond() >= rule.windowSeconds()
                        ? new Window(now, 1)
                        : new Window(old.startEpochSecond(), old.count() + 1));

        if (w.count() > rule.limit()) {
            long retryAfter = Math.max(1, rule.windowSeconds() - (now - w.startEpochSecond()));
            log.warn("Rate limit hit on {} from {}", rule.path(), clientIp(request));
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":"
                    + "\"Too many attempts. Please wait " + humanize(retryAfter) + " and try again.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Drops expired windows so the map doesn't grow without bound. */
    @Scheduled(fixedDelay = 600_000)
    public void evictExpired() {
        long now = System.currentTimeMillis() / 1000;
        windows.entrySet().removeIf(e -> now - e.getValue().startEpochSecond() >= 3600);
    }

    private Rule findRule(String path) {
        for (Rule r : rules) {
            if (r.path().equals(path)) return r;
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        if (trustProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static String humanize(long seconds) {
        return seconds >= 120 ? (seconds / 60) + " minutes" : seconds + " seconds";
    }
}
