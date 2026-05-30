package org.koikifw.libkoiki.batch.validation;

import java.util.Objects;

/**
 * A single validation failure: an optional {@code field} reference and a
 * human-readable {@code message}.
 *
 * @param field   the offending field/property name, or {@code null} for an
 *                object-level (cross-field) error
 * @param message human-readable description of the failure (required)
 */
public record ValidationError(String field, String message) {

    public ValidationError {
        Objects.requireNonNull(message, "message must not be null");
    }
}
