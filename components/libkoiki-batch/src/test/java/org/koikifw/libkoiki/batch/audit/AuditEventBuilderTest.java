package org.koikifw.libkoiki.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

class AuditEventBuilderTest {

    @Test
    void buildsEventWithAutoOccurredAt() {
        Instant before = Instant.now();
        AuditEvent event = AuditEventBuilder.builder()
                .eventType("TYPE")
                .message("msg")
                .build();
        Instant after = Instant.now();

        assertThat(event.occurredAt()).isBetween(before, after);
        assertThat(event.eventType()).isEqualTo("TYPE");
        assertThat(event.message()).isEqualTo("msg");
    }

    @Test
    void contextPopulatesFromJobExecution() {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.BIZ_DATE, "20260530")
                .addString(StandardJobParameters.REQUEST_ID, "req-1")
                .toJobParameters();
        JobExecution execution = mock(JobExecution.class);
        JobInstance instance = mock(JobInstance.class);
        when(instance.getJobName()).thenReturn("customer-daily-sync");
        when(execution.getJobInstance()).thenReturn(instance);
        when(execution.getId()).thenReturn(42L);
        when(execution.getJobParameters()).thenReturn(params);

        AuditEvent event = AuditEventBuilder.builder()
                .eventType("CUSTOMER_SYNCED")
                .message("done")
                .context(execution)
                .attribute("count", "100")
                .build();

        assertThat(event.jobName()).isEqualTo("customer-daily-sync");
        assertThat(event.jobExecutionId()).isEqualTo(42L);
        assertThat(event.bizDate()).isEqualTo("20260530");
        assertThat(event.requestId()).isEqualTo("req-1");
        assertThat(event.attributes()).containsExactlyEntriesOf(java.util.Map.of("count", "100"));
    }

    @Test
    void contextLeavesOptionalsNullWhenJobParametersAreEmpty() {
        JobExecution execution = mock(JobExecution.class);
        JobInstance instance = mock(JobInstance.class);
        when(instance.getJobName()).thenReturn("customer-daily-sync");
        when(execution.getJobInstance()).thenReturn(instance);
        when(execution.getId()).thenReturn(42L);
        when(execution.getJobParameters()).thenReturn(new JobParametersBuilder().toJobParameters());

        AuditEvent event = AuditEventBuilder.builder()
                .eventType("TYPE")
                .message("msg")
                .context(execution)
                .build();

        assertThat(event.jobName()).isEqualTo("customer-daily-sync");
        assertThat(event.jobExecutionId()).isEqualTo(42L);
        assertThat(event.bizDate()).isNull();
        assertThat(event.requestId()).isNull();
    }

    @Test
    void builderMutationsAfterBuildDoNotAffectEvent() {
        AuditEventBuilder builder = AuditEventBuilder.builder()
                .eventType("TYPE")
                .message("msg")
                .attribute("k", "v");
        AuditEvent event = builder.build();

        builder.attribute("k", "v2");
        builder.attribute("k2", "v3");

        assertThat(event.attributes()).containsExactlyEntriesOf(java.util.Map.of("k", "v"));
    }
}
