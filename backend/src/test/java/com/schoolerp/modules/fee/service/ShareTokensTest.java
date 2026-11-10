package com.schoolerp.modules.fee.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShareTokensTest {

    @Test
    void tokensAreUrlSafeAndUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String token = ShareTokens.newToken();
            assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
            assertThat(ShareTokens.looksValid(token)).isTrue();
            seen.add(token);
        }
        assertThat(seen).hasSize(1000);
    }

    @Test
    void rejectsObviouslyBadTokens() {
        assertThat(ShareTokens.looksValid(null)).isFalse();
        assertThat(ShareTokens.looksValid("")).isFalse();
        assertThat(ShareTokens.looksValid("SNVP-2026-0001")).isFalse();   // a receipt number
        assertThat(ShareTokens.looksValid("a".repeat(65))).isFalse();
        assertThat(ShareTokens.looksValid("abc/../" + "a".repeat(40))).isFalse();
    }
}
