package org.koikifw.refapp.batch.jobs.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
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
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, CustomerDailySyncJobConfig.JOB_NAME)
                .addString(StandardJobParameters.BIZ_DATE, "20260527")
                .toJobParameters();
        assertThatThrownBy(() -> jobOperator.start(customerDailySyncJob, params))
                .isInstanceOf(InvalidJobParametersException.class);
    }
}
