package org.koikifw.libkoiki.batch.execution;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.batch.core.job.parameters.JobParameters;

/**
 * Typed access to the standard KOIKI job parameters.
 */
public class JobParametersAccessor {

    private final JobParameters parameters;

    public JobParametersAccessor(JobParameters parameters) {
        this.parameters = parameters;
    }

    public String jobName() {
        return parameters.getString(StandardJobParameters.JOB_NAME);
    }

    public String requestId() {
        return parameters.getString(StandardJobParameters.REQUEST_ID);
    }

    public LocalDate bizDate() {
        String raw = parameters.getString(StandardJobParameters.BIZ_DATE);
        if (raw == null) {
            throw new IllegalStateException(StandardJobParameters.BIZ_DATE + " is missing");
        }
        try {
            return LocalDate.parse(raw, StandardJobParameters.BIZ_DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException(
                    StandardJobParameters.BIZ_DATE + " is not in yyyyMMdd format: " + raw, ex);
        }
    }
}
