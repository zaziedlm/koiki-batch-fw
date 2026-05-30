package org.koikifw.libkoiki.batch.validation;

import java.util.List;
import java.util.stream.Collectors;

import org.koikifw.libkoiki.batch.fault.BusinessException;

/**
 * Fail-fast validation failure. Extends {@link BusinessException} so the
 * framework's fault classifier maps it to business error / exit code {@code 20}.
 *
 * <p>Typically raised via {@link ValidationResult#throwIfInvalid(String)} when a
 * business precondition or input record is invalid and processing must stop.</p>
 */
public class ValidationException extends BusinessException {

    private final transient List<ValidationError> errors;

    public ValidationException(String context, List<ValidationError> errors) {
        super(buildMessage(context, errors));
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    /** The validation errors that caused this failure (never {@code null}). */
    public List<ValidationError> getErrors() {
        return errors;
    }

    private static String buildMessage(String context, List<ValidationError> errors) {
        String prefix = (context == null || context.isBlank()) ? "Validation failed" : context;
        if (errors == null || errors.isEmpty()) {
            return prefix;
        }
        String detail = errors.stream()
                .map(e -> (e.field() == null ? "" : e.field() + ": ") + e.message())
                .collect(Collectors.joining("; "));
        return prefix + " (" + errors.size() + " error(s)): " + detail;
    }
}
