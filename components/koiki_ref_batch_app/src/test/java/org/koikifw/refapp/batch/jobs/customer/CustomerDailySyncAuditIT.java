package org.koikifw.refapp.batch.jobs.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.koikifw.libkoiki.batch.audit.LoggingAuditEventPublisher;
import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Verifies that the customer-daily-sync tasklet emits a business audit event
 * through the framework's {@code AuditEventPublisher}, lands on the dedicated
 * {@code org.koikifw.audit} logger, and carries the {@code AUDIT} SLF4J marker.
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
class CustomerDailySyncAuditIT {

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private Job customerDailySyncJob;

    private ListAppender<ILoggingEvent> appender;

    private Logger auditLogger;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        auditLogger = (Logger) LoggerFactory.getLogger(LoggingAuditEventPublisher.AUDIT_LOGGER_NAME);
        auditLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        auditLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void taskletEmitsAuditEventOnSuccess() throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, CustomerDailySyncJobConfig.JOB_NAME)
                .addString(StandardJobParameters.BIZ_DATE, "20260530")
                .addString(StandardJobParameters.REQUEST_ID, UUID.randomUUID().toString())
                .toJobParameters();

        JobExecution execution = jobOperator.start(customerDailySyncJob, parameters);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        assertThat(appender.list)
                .as("audit logger receives at least one event from the tasklet")
                .isNotEmpty();

        boolean auditMatched = appender.list.stream().anyMatch(event ->
                event.getLevel() == Level.INFO
                        && event.getLoggerName().equals(LoggingAuditEventPublisher.AUDIT_LOGGER_NAME)
                        && event.getMarkerList().stream()
                                .anyMatch(m -> LoggingAuditEventPublisher.AUDIT_MARKER_NAME.equals(m.getName()))
                        && event.getFormattedMessage().contains("eventType=CUSTOMER_DAILY_SYNC_COMPLETED")
                        && event.getFormattedMessage()
                                .contains("jobName=" + CustomerDailySyncJobConfig.JOB_NAME));
        assertThat(auditMatched)
                .as("audit event with eventType=CUSTOMER_DAILY_SYNC_COMPLETED and matching jobName is present")
                .isTrue();
    }
}
