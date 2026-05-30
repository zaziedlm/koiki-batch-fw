package org.koikifw.libkoiki.batch.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class BatchCharsetsTest {

    @Test
    void resolvesKnownCharsets() {
        assertThat(BatchCharsets.resolve("UTF-8")).isEqualTo(StandardCharsets.UTF_8);
        assertThat(BatchCharsets.resolve("MS932").name()).isEqualTo("windows-31j");
    }

    @Test
    void trimsWhitespace() {
        assertThat(BatchCharsets.resolve("  UTF-8  ")).isEqualTo(StandardCharsets.UTF_8);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> BatchCharsets.resolve("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsupportedName() {
        assertThatThrownBy(() -> BatchCharsets.resolve("NO_SUCH_CHARSET"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NO_SUCH_CHARSET");
    }
}
