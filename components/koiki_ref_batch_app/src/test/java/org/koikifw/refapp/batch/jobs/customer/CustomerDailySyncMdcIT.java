package org.koikifw.refapp.batch.jobs.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.koikifw.libkoiki.batch.observability.CorrelationKeys;
import org.koikifw.libkoiki.batch.observability.JobLogListener;
import org.koikifw.libkoiki.batch.observability.StepLogListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Verifies that JobLogListener / StepLogListener populate the SLF4J MDC with
 * the KOIKI correlation keys during a real job execution, and that the MDC is
 * cleaned up after the job in both success and failure paths.
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(CustomerDailySyncMdcIT.MdcFailingJobConfig.class)
class CustomerDailySyncMdcIT {

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private Job customerDailySyncJob;

    @Autowired
    private Job mdcFailingJob;

    private ListAppender<ILoggingEvent> appender;

    private Logger koikiLogger;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        koikiLogger = (Logger) LoggerFactory.getLogger("org.koikifw");
        koikiLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        koikiLogger.detachAppender(appender);
        appender.stop();
        MDC.clear();
    }

    private JobParameters uniqueParameters(String jobName) {
        return new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, jobName)
                .addString(StandardJobParameters.BIZ_DATE, "20260528")
                .addString(StandardJobParameters.REQUEST_ID, UUID.randomUUID().toString())
                .toJobParameters();
    }

    private void assertNoCorrelationMdc() {
        assertThat(MDC.get(CorrelationKeys.JOB_NAME)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_EXEC_ID)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_BIZ_DATE)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_REQUEST_ID)).isNull();
        assertThat(MDC.get(CorrelationKeys.JOB_TENANT)).isNull();
        assertThat(MDC.get(CorrelationKeys.STEP_NAME)).isNull();
        assertThat(MDC.get(CorrelationKeys.STEP_EXEC_ID)).isNull();
    }

    @Test
    void jobExecutionPopulatesCorrelationMdc() throws Exception {
        JobParameters parameters = uniqueParameters(CustomerDailySyncJobConfig.JOB_NAME);
        String requestId = parameters.getString(StandardJobParameters.REQUEST_ID);

        JobExecution execution = jobOperator.start(customerDailySyncJob, parameters);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        boolean jobMdcSeen = appender.list.stream()
                .map(ILoggingEvent::getMDCPropertyMap)
                .anyMatch(m -> CustomerDailySyncJobConfig.JOB_NAME.equals(m.get(CorrelationKeys.JOB_NAME))
                        && m.containsKey(CorrelationKeys.JOB_EXEC_ID)
                        && "20260528".equals(m.get(CorrelationKeys.JOB_BIZ_DATE))
                        && requestId.equals(m.get(CorrelationKeys.JOB_REQUEST_ID)));
        assertThat(jobMdcSeen).as("at least one event carries the job MDC keys").isTrue();

        boolean stepMdcSeen = appender.list.stream()
                .map(ILoggingEvent::getMDCPropertyMap)
                .anyMatch(m -> "customer-daily-sync-step".equals(m.get(CorrelationKeys.STEP_NAME))
                        && m.containsKey(CorrelationKeys.STEP_EXEC_ID));
        assertThat(stepMdcSeen).as("at least one event carries the step MDC keys").isTrue();

        assertNoCorrelationMdc();
    }

    @Test
    void failedJobAlsoClearsMdc() throws Exception {
        JobParameters parameters = uniqueParameters("mdc-failing-job");

        JobExecution execution = jobOperator.start(mdcFailingJob, parameters);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertNoCorrelationMdc();
    }

    /**
     * Test-scoped failing job that wires the framework listeners so the
     * cleanup contract can be observed on a real failure path.
     *
     * <p>Uses {@link TestConfiguration} (not {@code @Configuration}) so the
     * Spring TestContext framework does not treat this nested class as the
     * default configuration source, which would suppress the @SpringBootApplication
     * auto-detection.</p>
     */
    @TestConfiguration
    static class MdcFailingJobConfig {

        @Bean
        Job mdcFailingJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                JobLogListener jobLogListener, StepLogListener stepLogListener) {
            Tasklet tasklet = (contribution, chunkContext) -> {
                throw new RuntimeException("mdc-failing-job boom");
            };
            Step step = new StepBuilder("mdc-failing-step", jobRepository)
                    .tasklet(tasklet, transactionManager)
                    .listener(stepLogListener)
                    .build();
            return new JobBuilder("mdc-failing-job", jobRepository)
                    .listener(jobLogListener)
                    .start(step)
                    .build();
        }
    }
}
