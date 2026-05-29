package org.koikifw.libkoiki.batch.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

class JobLogListenerTest {

    private final JobLogListener listener = new JobLogListener();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private JobExecution execution(JobParameters params) {
        JobExecution execution = mock(JobExecution.class);
        JobInstance instance = mock(JobInstance.class);
        lenient().when(instance.getJobName()).thenReturn("customer-daily-sync");
        when(execution.getJobInstance()).thenReturn(instance);
        when(execution.getId()).thenReturn(42L);
        when(execution.getJobParameters()).thenReturn(params);
        lenient().when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        lenient().when(execution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        return execution;
    }

    @Test
    void beforeJobPutsAllAvailableMdcKeys() {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.BIZ_DATE, "20260528")
                .addString(StandardJobParameters.REQUEST_ID, "req-1")
                .addString(CorrelationKeys.TENANT_PARAMETER, "tenant-a")
                .toJobParameters();

        listener.beforeJob(execution(params));

        assertThat(MDC.get(CorrelationKeys.JOB_NAME)).isEqualTo("customer-daily-sync");
        assertThat(MDC.get(CorrelationKeys.JOB_EXEC_ID)).isEqualTo("42");
        assertThat(MDC.get(CorrelationKeys.JOB_BIZ_DATE)).isEqualTo("20260528");
        assertThat(MDC.get(CorrelationKeys.JOB_REQUEST_ID)).isEqualTo("req-1");
        assertThat(MDC.get(CorrelationKeys.JOB_TENANT)).isEqualTo("tenant-a");
    }

    @Test
    void beforeJobSkipsAbsentOptionalKeys() {
        JobParameters params = new JobParametersBuilder().toJobParameters();

        listener.beforeJob(execution(params));

        assertThat(MDC.get(CorrelationKeys.JOB_NAME)).isEqualTo("customer-daily-sync");
        assertThat(MDC.get(CorrelationKeys.JOB_EXEC_ID)).isEqualTo("42");
        assertThat(MDC.get(CorrelationKeys.JOB_BIZ_DATE)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_REQUEST_ID)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_TENANT)).isNull();
    }

    @Test
    void afterJobClearsAllJobMdcKeys() {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.BIZ_DATE, "20260528")
                .addString(StandardJobParameters.REQUEST_ID, "req-1")
                .addString(CorrelationKeys.TENANT_PARAMETER, "tenant-a")
                .toJobParameters();
        JobExecution execution = execution(params);
        listener.beforeJob(execution);

        listener.afterJob(execution);

        assertThat(MDC.get(CorrelationKeys.JOB_NAME)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_EXEC_ID)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_BIZ_DATE)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_REQUEST_ID)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_TENANT)).isNull();
    }

    @Test
    void afterJobClearsMdcEvenWhenLoggingThrows() {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.BIZ_DATE, "20260528")
                .addString(StandardJobParameters.REQUEST_ID, "req-1")
                .addString(CorrelationKeys.TENANT_PARAMETER, "tenant-a")
                .toJobParameters();
        JobExecution execution = mock(JobExecution.class);
        JobInstance instance = mock(JobInstance.class);
        lenient().when(instance.getJobName()).thenReturn("customer-daily-sync");
        when(execution.getJobInstance()).thenReturn(instance);
        when(execution.getId()).thenReturn(42L);
        when(execution.getJobParameters()).thenReturn(params);
        when(execution.getStatus()).thenThrow(new RuntimeException("logging boom"));
        listener.beforeJob(execution);

        assertThatThrownBy(() -> listener.afterJob(execution))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("logging boom");

        assertThat(MDC.get(CorrelationKeys.JOB_NAME)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_EXEC_ID)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_BIZ_DATE)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_REQUEST_ID)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_TENANT)).isNull();
    }
}
