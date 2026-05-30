package org.koikifw.libkoiki.batch.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RedactingMaskerTest {

    @Test
    void redactsAnyNonNullValueToDefaultToken() {
        Masker masker = new RedactingMasker();

        assertThat(masker.mask("4111111111111111")).isEqualTo(RedactingMasker.DEFAULT_MASK);
        assertThat(masker.mask("alice@example.com")).isEqualTo(RedactingMasker.DEFAULT_MASK);
        assertThat(masker.mask("")).isEqualTo(RedactingMasker.DEFAULT_MASK);
    }

    @Test
    void returnsNullUnchanged() {
        assertThat(new RedactingMasker().mask(null)).isNull();
    }

    @Test
    void usesConfiguredToken() {
        Masker masker = new RedactingMasker("[REDACTED]");

        assertThat(masker.mask("secret")).isEqualTo("[REDACTED]");
    }

    @Test
    void fallsBackToDefaultTokenWhenBlankOrNull() {
        assertThat(new RedactingMasker(null).mask("x")).isEqualTo(RedactingMasker.DEFAULT_MASK);
        assertThat(new RedactingMasker("   ").mask("x")).isEqualTo(RedactingMasker.DEFAULT_MASK);
    }
}
