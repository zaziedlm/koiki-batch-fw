package org.koikifw.refapp.batch.jobs.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.koikifw.libkoiki.batch.io.AtomicOutputListener;
import org.koikifw.libkoiki.batch.io.FileIngestionLifecycleListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Confirms the ingestion charset is switchable via configuration: the same
 * {@code billing-file} job reads and writes a UTF-8 feed when
 * {@code koiki.batch.io.file.charset=UTF-8}.
 */
@SpringBootTest(properties = {"spring.batch.job.enabled=false", "koiki.batch.io.file.charset=UTF-8"})
class BillingFileUtf8IT {

    @TempDir
    static Path base;

    @DynamicPropertySource
    static void ioDirs(DynamicPropertyRegistry registry) {
        registry.add("koiki.batch.io.file.archive-dir", () -> base.resolve("archive").toString());
        registry.add("koiki.batch.io.file.error-dir", () -> base.resolve("error").toString());
    }

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private Job billingFileJob;

    @Test
    void readsAndWritesUtf8Feed() throws Exception {
        Path in = base.resolve("in");
        Files.createDirectories(in);
        Path input = in.resolve("utf8.dat");
        Files.write(input, "顧客009,9000\n".getBytes(StandardCharsets.UTF_8));
        Path output = base.resolve("out").resolve("utf8-out.dat");

        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, BillingFileJobConfig.JOB_NAME)
                .addString(StandardJobParameters.BIZ_DATE, "20260530")
                .addString(StandardJobParameters.REQUEST_ID, UUID.randomUUID().toString())
                .addString(FileIngestionLifecycleListener.INPUT_FILE_PARAMETER, input.toString())
                .addString(AtomicOutputListener.OUTPUT_FILE_PARAMETER, output.toString())
                .toJobParameters();

        JobExecution execution = jobOperator.start(billingFileJob, params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output).exists();
        assertThat(Files.readString(output, StandardCharsets.UTF_8)).contains("顧客009,9000");
    }
}
