package org.koikifw.libkoiki.batch.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MaskingPatternConverter}. The test lives in the same
 * package as the converter so it can call the protected {@code transform} method
 * directly (the {@link ch.qos.logback.classic.spi.ILoggingEvent} argument is
 * unused by the converter, so {@code null} is passed).
 */
class MaskingPatternConverterTest {

    private MaskingPatternConverter started(String... patterns) {
        MaskingPatternConverter converter = new MaskingPatternConverter();
        converter.setOptionList(List.of(patterns));
        converter.start();
        return converter;
    }

    @Test
    void masksSingleMatch() {
        MaskingPatternConverter converter = started("\\d{12,19}");

        assertThat(converter.transform(null, "card=4111111111111111 ok"))
                .isEqualTo("card=" + MaskingPatternConverter.DEFAULT_MASK + " ok");
    }

    @Test
    void masksAllOccurrencesAndMultiplePatterns() {
        MaskingPatternConverter converter = started("\\d{4}", "secret");

        assertThat(converter.transform(null, "1234 and 5678 secret"))
                .isEqualTo(MaskingPatternConverter.DEFAULT_MASK + " and "
                        + MaskingPatternConverter.DEFAULT_MASK + " "
                        + MaskingPatternConverter.DEFAULT_MASK);
    }

    @Test
    void isIdentityWithoutOptions() {
        MaskingPatternConverter converter = new MaskingPatternConverter();
        converter.start();

        assertThat(converter.transform(null, "nothing to mask 1234")).isEqualTo("nothing to mask 1234");
    }

    @Test
    void leavesNonMatchingTextUnchanged() {
        MaskingPatternConverter converter = started("\\d{12,19}");

        assertThat(converter.transform(null, "no long digits here")).isEqualTo("no long digits here");
    }

    @Test
    void nullInputReturnsNull() {
        MaskingPatternConverter converter = started("\\d+");

        assertThat(converter.transform(null, null)).isNull();
    }

    @Test
    void invalidPatternIsSkippedAndDoesNotThrow() {
        MaskingPatternConverter converter = new MaskingPatternConverter();
        converter.setOptionList(List.of("(unclosed", "\\d{4}"));

        assertThatCode(converter::start).doesNotThrowAnyException();
        // The valid pattern still masks; the invalid one was ignored.
        assertThat(converter.transform(null, "pin 1234")).isEqualTo("pin " + MaskingPatternConverter.DEFAULT_MASK);
    }
}
