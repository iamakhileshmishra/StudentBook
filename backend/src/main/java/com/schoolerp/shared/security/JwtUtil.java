package com.schoolerp.shared.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    private static final int MIN_SECRET_BYTES = 64;

    /** Secrets that are published in this repository; never acceptable outside dev. */
    private static final Set<String> PUBLIC_SECRETS = Set.of(
            "ThisIsADevSecretKeyThatMustBeChangedInProductionAndShouldBe64CharsLong!!!",
            "DevSecretKeyChangeInProductionMustBe64CharactersLongAtMinimumOk!",
            "CHANGE_ME_TO_A_RANDOM_64_CHARACTER_STRING_NEVER_SHARE_THIS_KEY!!!");

    private final SecretKey key;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtUtil(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${app.jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs,
            Environment environment) {
        validateSecret(secret, environment);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    /** Fail fast rather than sign tokens with a missing, short or publicly known key. */
    private static void validateSecret(String secret, Environment environment) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set. Configure a random secret of at least "
                    + MIN_SECRET_BYTES + " characters.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long.");
        }
        if (PUBLIC_SECRETS.contains(secret) && !environment.acceptsProfiles(Profiles.of("dev"))) {
            throw new IllegalStateException("JWT_SECRET is a publicly known example value. "
                    + "Generate a new random secret for this environment.");
        }
    }

    public String generateAccessToken(UUID userId, UUID schoolId, String role, String fullName, int tokenVersion) {
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of(
                        "schoolId", schoolId.toString(),
                        "role", role,
                        "name", fullName,
                        "type", "ACCESS",
                        "tv", tokenVersion
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiryMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId, UUID schoolId, int tokenVersion) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // jti — lets logout revoke this token
                .subject(userId.toString())
                .claims(Map.of(
                        "schoolId", schoolId.toString(),
                        "type", "REFRESH",
                        "tv", tokenVersion
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiryMs))
                .signWith(key)
                .compact();
    }

    public Claims validateAndExtractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Parses the token, or returns null if it is invalid, expired or tampered with. */
    public Claims parseOrNull(String token) {
        try {
            return validateAndExtractClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        return parseOrNull(token) != null;
    }

    /** Token version claim; tokens issued before V13 have none and count as version 0. */
    public static int tokenVersion(Claims claims) {
        Integer tv = claims.get("tv", Integer.class);
        return tv == null ? 0 : tv;
    }

    public long getRefreshTokenExpirySeconds() {
        return refreshTokenExpiryMs / 1000;
    }
}
