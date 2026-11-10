package com.schoolerp.modules.fee.service;

import java.security.SecureRandom;
import java.util.Base64;

/** Unguessable tokens for public receipt links (256 bits, URL-safe, 43 characters). */
public final class ShareTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private ShareTokens() {}

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** True if the value could be a token, so obviously bad input never reaches the database. */
    public static boolean looksValid(String token) {
        return token != null && token.matches("[A-Za-z0-9_-]{32,64}");
    }
}
