package org.koikifw.libkoiki.batch.execution;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersValidator;

/**
 * Validates that the standard KOIKI job parameters are present and well formed.
 *
 * <p>Presence of the required keys is delegated to {@link DefaultJobParametersValidator};
 * the {@code job.bizDate} format check is added on top.</p>
 */
public class KoikiJobParametersValidator implements JobParametersValidator {

    private final DefaultJobParametersValidator requiredKeysValidator = new DefaultJobParametersValidator(
            new String[] {StandardJobParameters.JOB_NAME, StandardJobParameters.BIZ_DATE,
                    StandardJobParameters.REQUEST_ID},
            new String[] {});

    @Override
    public void validate(JobParameters parameters) throws InvalidJobParametersException {
        requiredKeysValidator.validate(parameters);
        validateBizDate(parameters);
    }

    private void validateBizDate(JobParameters parameters) throws InvalidJobParametersException {
        String bizDate = parameters.getString(StandardJobParameters.BIZ_DATE);
        if (bizDate == null) {
            return;
        }
        try {
            LocalDate.parse(bizDate, StandardJobParameters.BIZ_DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new InvalidJobParametersException(
                    StandardJobParameters.BIZ_DATE + " must be in yyyyMMdd format but was: " + bizDate);
        }
    }
}
