package org.koikifw.refapp.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.koikifw.libkoiki.batch.execution.ConcurrencyGuardService;
import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.koikifw.libkoiki.batch.fault.BusinessException;
import org.koikifw.libkoiki.batch.fault.SystemException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * End-to-end verification that the framework's exit-code generation flows
 * through the real Spring Boot launch path (JobLauncherApplicationRunner ->
 * JobExecutionEvent -> KoikiBatchExitCodeGenerator -> SpringApplication.exit)
 * and produces the KOIKI process exit codes 0 / 20 / 30.
 */
class ExitCodeE2EIT {

    private int runJobAndResolveExitCode(String jobName) {
        return runJobAndResolveExitCode(jobName, KoikiRefBatchApplication.class, FailingJobsConfig.class);
    }

    private int runJobAndResolveExitCode(String jobName, Class<?>... sources) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(sources)
                .run(
                        "--spring.batch.job.name=" + jobName,
                        StandardJobParameters.JOB_NAME + "=" + jobName,
                        StandardJobParameters.BIZ_DATE + "=20260527",
                        StandardJobParameters.REQUEST_ID + "=" + UUID.randomUUID());
        try {
            return SpringApplication.exit(context);
        } finally {
            context.close();
        }
    }

    @Test
    void successfulJobExitsZero() {
        assertThat(runJobAndResolveExitCode("customer-daily-sync")).isEqualTo(0);
    }

    @Test
    void businessFailureExitsTwenty() {
        assertThat(runJobAndResolveExitCode("e2e-business-failure-job")).isEqualTo(20);
    }

    @Test
    void systemFailureExitsThirty() {
        assertThat(runJobAndResolveExitCode("e2e-system-failure-job")).isEqualTo(30);
    }

    /**
     * Verifies that the concurrency guard rejection path travels through
     * {@link org.koikifw.libkoiki.batch.execution.ConcurrencyGuardJobListener} →
     * {@link SystemException} → {@code KoikiBatchExitCodeGenerator} → exit 30.
     * The denying guard is supplied as a {@code @Primary} bean so it overrides
     * the framework's default {@link org.koikifw.libkoiki.batch.execution.JobRepositoryConcurrencyGuardService}.
     */
    @Test
    void concurrencyGuardRejectionExitsThirty() {
        int exit = runJobAndResolveExitCode(
                "customer-daily-sync",
                KoikiRefBatchApplication.class, DenyingGuardConfig.class);
        assertThat(exit).isEqualTo(30);
    }

    @Configuration
    static class FailingJobsConfig {

        @Bean
        Job e2eBusinessFailureJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
            return failingJob("e2e-business-failure-job", jobRepository, transactionManager,
                    () -> new BusinessException("e2e business failure"));
        }

        @Bean
        Job e2eSystemFailureJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
            return failingJob("e2e-system-failure-job", jobRepository, transactionManager,
                    () -> new SystemException("e2e system failure"));
        }

        private Job failingJob(String name, JobRepository jobRepository,
                PlatformTransactionManager transactionManager, java.util.function.Supplier<RuntimeException> failure) {
            Tasklet tasklet = (contribution, chunkContext) -> {
                throw failure.get();
            };
            Step step = new StepBuilder(name + "-step", jobRepository)
                    .tasklet(tasklet, transactionManager)
                    .build();
            return new JobBuilder(name, jobRepository).start(step).build();
        }
    }

    @Configuration
    static class DenyingGuardConfig {

        @Bean
        @Primary
        ConcurrencyGuardService denyingConcurrencyGuardService() {
            return execution -> false;
        }
    }
}
