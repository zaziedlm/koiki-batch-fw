package org.koikifw.refapp.batch.jobs.customer;

import org.koikifw.libkoiki.batch.audit.AuditEventBuilder;
import org.koikifw.libkoiki.batch.audit.AuditEventPublisher;
import org.koikifw.libkoiki.batch.execution.ConcurrencyGuardJobListener;
import org.koikifw.libkoiki.batch.execution.JobParametersAccessor;
import org.koikifw.libkoiki.batch.execution.KoikiJobParametersValidator;
import org.koikifw.libkoiki.batch.observability.JobLogListener;
import org.koikifw.libkoiki.batch.observability.StepLogListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Reference job that demonstrates the libkoiki-batch Phase 0 framework features:
 * the standard job parameter validator and the typed parameter accessor.
 */
@Configuration
public class CustomerDailySyncJobConfig {

    private static final Logger log = LoggerFactory.getLogger(CustomerDailySyncJobConfig.class);

    public static final String JOB_NAME = "customer-daily-sync";

    @Bean
    public Tasklet customerDailySyncTasklet(AuditEventPublisher auditEventPublisher) {
        return (contribution, chunkContext) -> {
            var jobExecution = contribution.getStepExecution().getJobExecution();
            JobParameters parameters = jobExecution.getJobParameters();
            JobParametersAccessor accessor = new JobParametersAccessor(parameters);
            log.info("{} running: bizDate={}, requestId={}", JOB_NAME, accessor.bizDate(), accessor.requestId());

            auditEventPublisher.publish(AuditEventBuilder.builder()
                    .eventType("CUSTOMER_DAILY_SYNC_COMPLETED")
                    .message("customer-daily-sync completed")
                    .context(jobExecution)
                    .build());

            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step customerDailySyncStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            Tasklet customerDailySyncTasklet, StepLogListener stepLogListener) {
        return new StepBuilder("customer-daily-sync-step", jobRepository)
                .tasklet(customerDailySyncTasklet, transactionManager)
                .listener(stepLogListener)
                .build();
    }

    @Bean
    public Job customerDailySyncJob(JobRepository jobRepository, Step customerDailySyncStep,
            KoikiJobParametersValidator koikiJobParametersValidator,
            ConcurrencyGuardJobListener concurrencyGuardJobListener,
            JobLogListener jobLogListener) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(koikiJobParametersValidator)
                .listener(concurrencyGuardJobListener)
                .listener(jobLogListener)
                .start(customerDailySyncStep)
                .build();
    }
}
