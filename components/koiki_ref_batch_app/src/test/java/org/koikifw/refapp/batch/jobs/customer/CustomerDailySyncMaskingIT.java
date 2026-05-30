package org.koikifw.refapp.batch.jobs.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.koikifw.libkoiki.batch.audit.LoggingAuditEventPublisher;
import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.koikifw.libkoiki.batch.security.RedactingMasker;
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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Verifies that the framework's value-level masking (configured via
 * {@code koiki.batch.security.masking.sensitive-keys} in {@code application.yml})
 * redacts the sensitive {@code accountId} attribute in audit output, while
 * non-sensitive attributes remain in the clear.
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
class CustomerDailySyncMaskingIT {

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
    void sensitiveAttributeIsMaskedInAuditOutput() throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, CustomerDailySyncJobConfig.JOB_NAME)
                .addString(StandardJobParameters.BIZ_DATE, "20260530")
                .addString(StandardJobParameters.REQUEST_ID, UUID.randomUUID().toString())
                .toJobParameters();

        JobExecution execution = jobOperator.start(customerDailySyncJob, parameters);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        String auditLine = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.contains("eventType=CUSTOMER_DAILY_SYNC_COMPLETED"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("audit event was not emitted"));

        assertThat(auditLine)
                .as("sensitive accountId value is masked and never appears in the clear")
                .doesNotContain("ACC-0001-DEMO")
                .contains("attr." + CustomerDailySyncJobConfig.SENSITIVE_ACCOUNT_ATTRIBUTE
                        + "=\"" + RedactingMasker.DEFAULT_MASK + "\"")
                .as("non-sensitive attribute stays in the clear")
                .contains("attr.customerCount=\"1\"");
    }
}
