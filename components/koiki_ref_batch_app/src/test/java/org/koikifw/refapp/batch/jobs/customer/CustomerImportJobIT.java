package org.koikifw.refapp.batch.jobs.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.koikifw.libkoiki.batch.fault.DefaultFaultClassifier;
import org.koikifw.libkoiki.batch.fault.FaultCategory;
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
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for the DB-backed {@code customer-import} chunk job in
 * <b>JDBC repository</b> mode ({@code jdbc-repository} profile) against H2.
 * Verifies that valid records are committed to the destination table with an
 * explicit commit boundary and that step metadata is persisted to the JDBC
 * {@code JobRepository} (the restart basis), and that an invalid record fails
 * the job as a business error (exit code 20) and rolls back.
 *
 * <p>See {@link CustomerImportResourcelessIT} for the same job running on the
 * default {@code ResourcelessJobRepository}.</p>
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@ActiveProfiles("jdbc-repository")
class CustomerImportJobIT {

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private Job customerImportJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final DefaultFaultClassifier faultClassifier = new DefaultFaultClassifier();

    @BeforeEach
    void cleanBusinessTables() {
        jdbcTemplate.update("DELETE FROM customer");
        jdbcTemplate.update("DELETE FROM customer_input");
    }

    private void seedInput(long id, String externalId, String email) {
        jdbcTemplate.update("INSERT INTO customer_input (id, external_id, email, created_at) VALUES (?, ?, ?, ?)",
                id, externalId, email, Timestamp.from(Instant.now()));
    }

    private JobParameters params() {
        return new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, CustomerImportJobConfig.JOB_NAME)
                .addString(StandardJobParameters.BIZ_DATE, "20260530")
                .addString(StandardJobParameters.REQUEST_ID, UUID.randomUUID().toString())
                .toJobParameters();
    }

    @Test
    void validRecordsAreImportedAndCommitted() throws Exception {
        seedInput(1, "EXT-1", "alice@example.com");
        seedInput(2, "EXT-2", "bob@example.com");
        seedInput(3, "EXT-3", "carol@example.com");

        JobExecution execution = jobOperator.start(customerImportJob, params());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Integer imported = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer", Integer.class);
        assertThat(imported).isEqualTo(3);

        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getWriteCount()).isEqualTo(3);

        // Step metadata is persisted to the JDBC JobRepository (restart basis). Scope the
        // query to this execution's step so it is independent of other test runs sharing
        // the in-memory database.
        Long persistedWriteCount = jdbcTemplate.queryForObject(
                "SELECT WRITE_COUNT FROM BATCH_STEP_EXECUTION WHERE STEP_EXECUTION_ID = ?",
                Long.class, step.getId());
        assertThat(persistedWriteCount).isEqualTo(3L);
    }

    @Test
    void invalidRecordFailsAsBusinessErrorAndRollsBack() throws Exception {
        seedInput(1, "EXT-1", "alice@example.com");
        seedInput(2, "EXT-2", "not-an-email");

        JobExecution execution = jobOperator.start(customerImportJob, params());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);

        // Single shared transaction: the whole chunk rolled back, nothing committed.
        Integer imported = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer", Integer.class);
        assertThat(imported).isZero();

        assertThat(execution.getAllFailureExceptions()).isNotEmpty();
        assertThat(faultClassifier.classify(execution.getAllFailureExceptions().get(0)))
                .as("validation failure maps to business error / exit code 20")
                .isEqualTo(FaultCategory.BUSINESS_ERROR);
    }
}
