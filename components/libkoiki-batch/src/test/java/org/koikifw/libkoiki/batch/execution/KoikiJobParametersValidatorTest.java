package org.koikifw.libkoiki.batch.execution;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

class KoikiJobParametersValidatorTest {

    private final KoikiJobParametersValidator validator = new KoikiJobParametersValidator();

    private JobParametersBuilder valid() {
        return new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, "customer-daily-sync")
                .addString(StandardJobParameters.BIZ_DATE, "20260527")
                .addString(StandardJobParameters.REQUEST_ID, "req-001");
    }

    @Test
    void acceptsValidParameters() {
        assertThatCode(() -> validator.validate(valid().toJobParameters())).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingRequestId() {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, "customer-daily-sync")
                .addString(StandardJobParameters.BIZ_DATE, "20260527")
                .toJobParameters();
        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(InvalidJobParametersException.class);
    }

    @Test
    void rejectsMalformedBizDate() {
        JobParameters params = valid()
                .addString(StandardJobParameters.BIZ_DATE, "2026-05-27")
                .toJobParameters();
        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessageContaining(StandardJobParameters.BIZ_DATE);
    }
}
