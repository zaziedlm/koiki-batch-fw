package org.koikifw.libkoiki.batch.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

class JobParametersAccessorTest {

    @Test
    void exposesTypedValues() {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, "customer-daily-sync")
                .addString(StandardJobParameters.BIZ_DATE, "20260527")
                .addString(StandardJobParameters.REQUEST_ID, "req-001")
                .toJobParameters();

        JobParametersAccessor accessor = new JobParametersAccessor(params);

        assertThat(accessor.jobName()).isEqualTo("customer-daily-sync");
        assertThat(accessor.requestId()).isEqualTo("req-001");
        assertThat(accessor.bizDate()).isEqualTo(LocalDate.of(2026, 5, 27));
    }

    @Test
    void failsOnMalformedBizDate() {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.BIZ_DATE, "2026/05/27")
                .toJobParameters();

        assertThatThrownBy(() -> new JobParametersAccessor(params).bizDate())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failsOnImpossibleCalendarBizDate() {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.BIZ_DATE, "20260230")
                .toJobParameters();

        assertThatThrownBy(() -> new JobParametersAccessor(params).bizDate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(StandardJobParameters.BIZ_DATE);
    }

    @Test
    void failsWhenBizDateMissing() {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, "customer-daily-sync")
                .addString(StandardJobParameters.REQUEST_ID, "req-001")
                .toJobParameters();

        assertThatThrownBy(() -> new JobParametersAccessor(params).bizDate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(StandardJobParameters.BIZ_DATE);
    }

    @Test
    void returnsNullForUnsetJobNameAndRequestId() {
        JobParameters params = new JobParametersBuilder().toJobParameters();
        JobParametersAccessor accessor = new JobParametersAccessor(params);

        assertThat(accessor.jobName()).isNull();
        assertThat(accessor.requestId()).isNull();
    }
}
