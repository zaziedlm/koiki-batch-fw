package org.koikifw.libkoiki.batch.validation;

/**
 * Reusable validation contract for an input record or a business precondition.
 *
 * <p>This is distinct from Spring Batch's
 * {@code org.springframework.batch.core.job.parameters.JobParametersValidator},
 * which validates job parameters at launch (the framework already provides
 * {@code KoikiJobParametersValidator} for that). A {@code Validator<T>} is
 * applied <em>inside</em> processing — for example in a chunk
 * {@code ItemProcessor} or a domain service.</p>
 *
 * <p>Validation is non-throwing: it returns a {@link ValidationResult} so the
 * caller can decide whether to skip the item or fail fast (via
 * {@link ValidationResult#throwIfInvalid(String)}).</p>
 *
 * @param <T> the type being validated
 */
@FunctionalInterface
public interface Validator<T> {

    /**
     * Validates {@code target} and returns the outcome. Implementations must not
     * throw for ordinary validation failures; they accumulate them in the result.
     */
    ValidationResult validate(T target);
}
