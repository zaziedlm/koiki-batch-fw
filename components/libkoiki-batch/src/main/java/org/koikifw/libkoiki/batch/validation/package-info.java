/**
 * Validation support for job parameters, input records, and business rule
 * preconditions that are reusable across batch applications.
 *
 * <p>Three validation kinds are kept in distinct places:</p>
 * <ul>
 *   <li><b>Job parameters</b> — validated at launch by
 *       {@code org.koikifw.libkoiki.batch.execution.KoikiJobParametersValidator}
 *       through Spring Batch's {@code JobParametersValidator}.</li>
 *   <li><b>Input records</b> — validated inside processing with
 *       {@link org.koikifw.libkoiki.batch.validation.Validator} (e.g. in a chunk
 *       {@code ItemProcessor}).</li>
 *   <li><b>Business preconditions</b> — cross-entity / DB-dependent rules, also
 *       expressed as a {@link org.koikifw.libkoiki.batch.validation.Validator};
 *       a violation throws
 *       {@link org.koikifw.libkoiki.batch.validation.ValidationException}
 *       (mapped to business error / exit code {@code 20}).</li>
 * </ul>
 *
 * <p>The contract is deliberately lightweight and free of external dependencies.
 * Applications that prefer annotation-driven Jakarta Bean Validation can run it
 * themselves and surface failures through {@code ValidationException}.</p>
 */
package org.koikifw.libkoiki.batch.validation;
