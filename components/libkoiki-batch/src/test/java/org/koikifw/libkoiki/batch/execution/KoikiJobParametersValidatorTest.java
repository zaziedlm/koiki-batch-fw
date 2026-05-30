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

    @Test
    void rejectsBizDateWithValidFormatButImpossibleCalendarDate() {
        JobParameters params = valid()
                .addString(StandardJobParameters.BIZ_DATE, "20260230")
                .toJobParameters();
        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessageContaining(StandardJobParameters.BIZ_DATE);
    }

    @Test
    void rejectsMissingJobName() {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.BIZ_DATE, "20260527")
                .addString(StandardJobParameters.REQUEST_ID, "req-001")
                .toJobParameters();
        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(InvalidJobParametersException.class);
    }

    @Test
    void rejectsBlankBizDate() {
        JobParameters params = valid()
                .addString(StandardJobParameters.BIZ_DATE, "   ")
                .toJobParameters();
        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessageContaining(StandardJobParameters.BIZ_DATE);
    }

    /**
     * Locks the contract that downstream apps may pass additional parameters
     * (incrementer-injected {@code run.id}, customer-app-specific keys, etc.)
     * without being rejected. The composition leaves {@code optionalKeys} empty
     * which {@code DefaultJobParametersValidator} interprets as "no enforcement
     * on extras"; only the three required keys are enforced.
     */
    @Test
    void acceptsExtraUnknownKeys() {
        JobParameters params = valid()
                .addString("customer.region", "JP-EAST")
                .addLong("run.id", 1L)
                .toJobParameters();
        assertThatCode(() -> validator.validate(params)).doesNotThrowAnyException();
    }
}
