package com.schoolerp.shared.config;

import com.schoolerp.shared.security.JwtAuthFilter;
import com.schoolerp.shared.security.RateLimitFilter;
import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Roles from users.role (SUPER_ADMIN | SCHOOL_ADMIN | FEE_COLLECTOR). */
    private static final String[] ADMINS = {"SCHOOL_ADMIN", "SUPER_ADMIN"};
    private static final String[] ALL_STAFF = {"SCHOOL_ADMIN", "SUPER_ADMIN", "FEE_COLLECTOR"};

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/forgot-password",
                                "/api/auth/forgot-password/**",
                                "/api/auth/refresh",
                                "/api/auth/logout", // acts on the SameSite=Strict refresh cookie only
                                "/api/health",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // ── Role-based access (test&bug.md SEC-05) ──────────────────
                        // Every signed-in user may manage their own password
                        .requestMatchers(HttpMethod.POST,
                                "/api/settings/change-password",
                                "/api/settings/change-password/request-otp").authenticated()
                        // Day-to-day fee desk work: read school data, collect fees
                        .requestMatchers(HttpMethod.GET,
                                "/api/dashboard/**",
                                "/api/students", "/api/students/*",
                                "/api/structure/**",
                                "/api/fee-structure", "/api/fee-structure/**",
                                "/api/fees/collection/dues",
                                "/api/payments/**",
                                "/api/settings/school",
                                "/api/settings/academic-years/current").hasAnyRole(ALL_STAFF)
                        .requestMatchers(HttpMethod.POST, "/api/fees/collection").hasAnyRole(ALL_STAFF)
                        // Everything else (setup, student changes, promotion, backup) is admin-only
                        .anyRequest().hasAnyRole(ADMINS)
                )
                // JSON 401/403 (the default for stateless APIs is a bare 403 for both), so the
                // frontend can tell "session expired → refresh" apart from "not allowed"
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> writeJson(res, 401,
                                "Unauthorized", "Authentication required"))
                        .accessDeniedHandler((req, res, e) -> writeJson(res, 403,
                                "Forbidden", "You do not have permission to perform this action"))
                )
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeJson(jakarta.servlet.http.HttpServletResponse res, int status, String error, String message)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // Required for httpOnly cookie (refresh token)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
