package org.koikifw.libkoiki.batch.security;

/**
 * Masks a single sensitive value before it is written to a log or audit record.
 *
 * <p>This is the framework's minimal masking SPI: it transforms one value and
 * has no knowledge of <em>which</em> values are sensitive. Deciding which fields
 * to mask is the caller's responsibility (for audit output, the configured set
 * of sensitive attribute keys in {@code koiki.batch.security.masking}).</p>
 *
 * <p>Applications that need value-class-specific behaviour (for example partial
 * masking that keeps the last few digits, or different rules per personal-data
 * class) provide their own implementation as a Spring bean; the framework's
 * auto-configuration backs off when a {@code Masker} bean is already present.</p>
 */
@FunctionalInterface
public interface Masker {

    /**
     * Returns a masked representation of {@code value}.
     *
     * @param value the raw value to mask; implementations should tolerate
     *              {@code null} (the default {@link RedactingMasker} returns
     *              {@code null} unchanged)
     * @return the masked value
     */
    String mask(String value);
}
