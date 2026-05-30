package org.koikifw.libkoiki.batch.security;

/**
 * Default {@link Masker} that fully redacts any non-null value by replacing it
 * with a fixed mask token (default {@value #DEFAULT_MASK}).
 *
 * <p>{@code null} is returned unchanged so callers that omit absent optional
 * fields keep doing so. This implementation intentionally carries no
 * value-class-specific rules (such as keeping the last few digits); those are
 * deferred and can be supplied by an application-provided {@link Masker} bean.</p>
 */
public final class RedactingMasker implements Masker {

    /** Mask token used when none is configured. */
    public static final String DEFAULT_MASK = "***";

    private final String mask;

    /** Creates a masker that redacts to {@link #DEFAULT_MASK}. */
    public RedactingMasker() {
        this(DEFAULT_MASK);
    }

    /**
     * Creates a masker that redacts to the given token.
     *
     * @param mask the replacement token; falls back to {@link #DEFAULT_MASK}
     *             when {@code null} or blank
     */
    public RedactingMasker(String mask) {
        this.mask = (mask == null || mask.isBlank()) ? DEFAULT_MASK : mask;
    }

    @Override
    public String mask(String value) {
        return value == null ? null : mask;
    }
}
