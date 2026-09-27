package com.schoolerp.shared.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Authenticates requests carrying a valid ACCESS token.
 *
 * An invalid or expired token is ignored rather than rejected here, so public
 * endpoints (login, refresh, …) still work and Spring Security returns 401 for
 * protected ones (SEC-16 / BUG-01). Each request re-checks the user in the
 * database so password changes, deactivation and role changes apply at once
 * (SEC-04), and users who must replace a temporary password can do nothing
 * else first (SEC-10).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Endpoints still reachable while must_change_password is set. */
    private static final Set<String> PASSWORD_CHANGE_ALLOWED = Set.of(
            "/api/settings/change-password");

    private final JwtUtil jwtUtil;
    private final JdbcTemplate jdbcTemplate;

    private record UserState(UUID schoolId, String role, int tokenVersion, boolean mustChangePassword) {}

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            Claims claims = token != null ? jwtUtil.parseOrNull(token) : null;

            if (claims != null && "ACCESS".equals(claims.get("type", String.class))) {
                UUID userId = UUID.fromString(claims.getSubject());
                UserState state = loadActiveUser(userId);

                if (state != null && state.tokenVersion() == JwtUtil.tokenVersion(claims)) {
                    if (state.mustChangePassword() && !isAllowedDuringPasswordChange(request)) {
                        writeError(response, HttpServletResponse.SC_FORBIDDEN, "PASSWORD_CHANGE_REQUIRED",
                                "Please set a new password before continuing.");
                        return;
                    }

                    TenantContext.setSchoolId(state.schoolId());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId.toString(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + state.role())));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // Clear AFTER the full request has been processed
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /** Current user state, or null if the user or their school is gone or inactive. */
    private UserState loadActiveUser(UUID userId) {
        List<UserState> rows = jdbcTemplate.query("""
                SELECT u.school_id, u.role, u.token_version, u.must_change_password
                FROM users u JOIN schools s ON s.id = u.school_id
                WHERE u.id = ? AND u.is_active = TRUE AND s.is_active = TRUE
                """,
                (rs, i) -> new UserState(
                        (UUID) rs.getObject("school_id"),
                        rs.getString("role"),
                        rs.getInt("token_version"),
                        rs.getBoolean("must_change_password")),
                userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static boolean isAllowedDuringPasswordChange(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PASSWORD_CHANGE_ALLOWED.contains(path) || path.startsWith("/api/auth/");
    }

    private static void writeError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
