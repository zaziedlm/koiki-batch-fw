package org.koikifw.refapp.batch.jobs.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.koikifw.libkoiki.batch.fault.KoikiExitCodeExceptionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
class CustomerDailySyncJobIT {

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private Job customerDailySyncJob;

    @Autowired
    private KoikiExitCodeExceptionMapper exitCodeExceptionMapper;

    private JobParametersBuilder validParameters() {
        return new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, CustomerDailySyncJobConfig.JOB_NAME)
                .addString(StandardJobParameters.BIZ_DATE, "20260527")
                .addString(StandardJobParameters.REQUEST_ID, UUID.randomUUID().toString());
    }

    @Test
    void completesWithValidParameters() throws Exception {
        JobExecution execution = jobOperator.start(customerDailySyncJob, validParameters().toJobParameters());
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void rejectsMissingRequestId() {
        // Distinct bizDate per invalid-parameter test: with the JDBC JobRepository the
        // launch persists a JobInstance before validation fails, so identical parameters
        // across tests would collide. Production launches use a unique requestId.
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, CustomerDailySyncJobConfig.JOB_NAME)
                .addString(StandardJobParameters.BIZ_DATE, "20260601")
                .toJobParameters();
        assertThatThrownBy(() -> jobOperator.start(customerDailySyncJob, params))
                .isInstanceOf(InvalidJobParametersException.class);
    }

    /**
     * The framework's {@link KoikiExitCodeExceptionMapper} is what makes the
     * Spring Boot launch path return exit 20 when {@code JobLauncherApplicationRunner}
     * fails with {@link InvalidJobParametersException} before any
     * {@code JobExecution} (and therefore any {@code JobExecutionEvent}) exists.
     * This verifies the registered mapper bean — produced by the auto-configuration
     * and consumed by Boot's exit-code resolution — really resolves that exception
     * chain to 20.
     */
    @Test
    void invalidParametersResolveToBusinessErrorExitCodeViaMapper() {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, CustomerDailySyncJobConfig.JOB_NAME)
                .addString(StandardJobParameters.BIZ_DATE, "20260602")
                .toJobParameters();

        InvalidJobParametersException thrown = catchInvalidJobParametersException(params);

        assertThat(exitCodeExceptionMapper.getExitCode(thrown)).isEqualTo(20);
    }

    private InvalidJobParametersException catchInvalidJobParametersException(JobParameters params) {
        try {
            jobOperator.start(customerDailySyncJob, params);
            throw new AssertionError("Expected InvalidJobParametersException");
        } catch (InvalidJobParametersException ex) {
            return ex;
        } catch (Exception ex) {
            throw new AssertionError("Expected InvalidJobParametersException but got " + ex.getClass(), ex);
        }
    }
}
