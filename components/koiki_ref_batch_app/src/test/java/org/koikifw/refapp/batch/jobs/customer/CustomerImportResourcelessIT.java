package org.koikifw.refapp.batch.jobs.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Demonstrates a <b>resourceless business batch</b>: the {@code customer-import}
 * chunk job runs on Spring Batch 6's default {@code ResourcelessJobRepository}
 * (no {@code jdbc-repository} profile), yet still reads and writes business data
 * through the H2 {@code DataSource} with a real transaction manager.
 *
 * <p>This proves the framework supports business RDBMS processing without a
 * metadata database: business rows are committed, while batch metadata is NOT
 * persisted to the {@code BATCH_*} tables (so there is no restart-from-failure).
 * See {@link CustomerImportJobIT} for the JDBC-repository counterpart.</p>
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
class CustomerImportResourcelessIT {

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private Job customerImportJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanBusinessTables() {
        jdbcTemplate.update("DELETE FROM customer");
        jdbcTemplate.update("DELETE FROM customer_input");
    }

    @Test
    void businessDataIsCommittedWithoutPersistingMetadata() throws Exception {
        jdbcTemplate.update("INSERT INTO customer_input (id, external_id, email, created_at) VALUES (?, ?, ?, ?)",
                1, "EXT-1", "alice@example.com", Timestamp.from(Instant.now()));
        jdbcTemplate.update("INSERT INTO customer_input (id, external_id, email, created_at) VALUES (?, ?, ?, ?)",
                2, "EXT-2", "bob@example.com", Timestamp.from(Instant.now()));

        JobParameters parameters = new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, CustomerImportJobConfig.JOB_NAME)
                .addString(StandardJobParameters.BIZ_DATE, "20260530")
                .addString(StandardJobParameters.REQUEST_ID, UUID.randomUUID().toString())
                .toJobParameters();

        JobExecution execution = jobOperator.start(customerImportJob, parameters);

        // Business RDBMS processing works: the job completes and rows are committed.
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Integer imported = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer", Integer.class);
        assertThat(imported).isEqualTo(2);

        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getWriteCount()).isEqualTo(2);

        // The default JobRepository is resourceless: nothing is written to the
        // BATCH_* metadata tables (they exist via Flyway but stay empty), so there
        // is no persisted execution history / restart basis.
        Integer persistedSteps = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BATCH_STEP_EXECUTION", Integer.class);
        assertThat(persistedSteps)
                .as("resourceless repository does not persist step metadata")
                .isZero();
    }
}
