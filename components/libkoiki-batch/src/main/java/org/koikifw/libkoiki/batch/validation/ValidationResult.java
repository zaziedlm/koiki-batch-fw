package org.koikifw.libkoiki.batch.validation;

import java.util.List;

/**
 * Immutable outcome of a {@link Validator} run: the (possibly empty) list of
 * {@link ValidationError}s.
 *
 * <p>The result is non-throwing by design so callers can choose how to react —
 * for example, skip an invalid item in a chunk step, or fail fast via
 * {@link #throwIfInvalid(String)} for a business precondition.</p>
 *
 * @param errors validation errors; never {@code null}, made immutable
 */
public record ValidationResult(List<ValidationError> errors) {

    public ValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    /** Returns {@code true} when there are no errors. */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /** A successful result with no errors. */
    public static ValidationResult valid() {
        return new ValidationResult(List.of());
    }

    /** A result wrapping the given errors. */
    public static ValidationResult of(List<ValidationError> errors) {
        return new ValidationResult(errors);
    }

    /** A failed result with a single field error. */
    public static ValidationResult withError(String field, String message) {
        return new ValidationResult(List.of(new ValidationError(field, message)));
    }

    /**
     * Throws a {@link ValidationException} (mapped to business error / exit code
     * {@code 20}) when this result is invalid; otherwise returns normally.
     *
     * @param context short description of what was validated, used in the
     *                exception message
     */
    public void throwIfInvalid(String context) {
        if (!isValid()) {
            throw new ValidationException(context, errors);
        }
    }
}
