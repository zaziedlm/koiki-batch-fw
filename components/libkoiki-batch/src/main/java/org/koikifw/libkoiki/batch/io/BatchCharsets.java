package org.koikifw.libkoiki.batch.io;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Resolves the configured ingestion/output charset name (e.g.
 * {@code koiki.batch.io.file.charset}) into a {@link Charset}, failing with a
 * clear message when the name is invalid or unsupported.
 *
 * <p>Lets jobs switch between legacy {@code MS932} (Shift_JIS) and modern
 * {@code UTF-8} feeds purely through configuration.</p>
 */
public final class BatchCharsets {

    private BatchCharsets() {
    }

    /**
     * Resolves {@code name} to a {@link Charset}.
     *
     * @throws IllegalArgumentException if {@code name} is blank, syntactically
     *         invalid, or names an unsupported charset
     */
    public static Charset resolve(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Charset name must not be blank");
        }
        try {
            return Charset.forName(name.trim());
        } catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
            throw new IllegalArgumentException("Unsupported charset: " + name, ex);
        }
    }
}
