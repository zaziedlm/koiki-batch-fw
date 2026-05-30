package org.koikifw.refapp.batch.jobs.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.koikifw.libkoiki.batch.fault.DefaultFaultClassifier;
import org.koikifw.libkoiki.batch.fault.FaultCategory;
import org.koikifw.libkoiki.batch.io.AtomicFileOutput;
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
 * Integration test for the {@code billing-file} file&rarr;file job, exercising the
 * Phase 5 IO value-adds with the default {@code MS932} charset: input-file
 * lifecycle (archive on success / error on failure + existence check) and atomic
 * output (temp promoted only on success). A multibyte {@code customerId} proves
 * the configured charset is actually applied end to end.
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
class BillingFileJobIT {

    private static final Charset MS932 = Charset.forName("MS932");

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

    private final DefaultFaultClassifier faultClassifier = new DefaultFaultClassifier();

    private Path writeInput(String name, String content) throws IOException {
        Path in = base.resolve("in");
        Files.createDirectories(in);
        Path file = in.resolve(name);
        Files.write(file, content.getBytes(MS932));
        return file;
    }

    private JobExecution run(Path input, Path output) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString(StandardJobParameters.JOB_NAME, BillingFileJobConfig.JOB_NAME)
                .addString(StandardJobParameters.BIZ_DATE, "20260530")
                .addString(StandardJobParameters.REQUEST_ID, UUID.randomUUID().toString())
                .addString(FileIngestionLifecycleListener.INPUT_FILE_PARAMETER, input.toString())
                .addString(AtomicOutputListener.OUTPUT_FILE_PARAMETER, output.toString())
                .toJobParameters();
        return jobOperator.start(billingFileJob, params);
    }

    @Test
    void successPromotesOutputAndArchivesInput() throws Exception {
        Path input = writeInput("billing.dat", "顧客001,1000\n顧客002,2000\n");
        Path output = base.resolve("out").resolve("billing-out.dat");

        JobExecution execution = run(input, output);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        // Atomic output: final file present, temp gone.
        assertThat(output).exists();
        assertThat(AtomicFileOutput.tempPath(output, ".inprogress")).doesNotExist();
        assertThat(Files.readString(output, MS932))
                .contains("顧客001,1000")
                .contains("顧客002,2000");
        // Input lifecycle: source consumed, archived.
        assertThat(input).doesNotExist();
        try (var archived = Files.list(base.resolve("archive"))) {
            assertThat(archived.filter(p -> p.getFileName().toString().endsWith("billing.dat"))).hasSize(1);
        }
    }

    @Test
    void missingInputFailsAsBusinessError() throws Exception {
        Path input = base.resolve("in").resolve("nope.dat");
        Path output = base.resolve("out").resolve("nope-out.dat");

        JobExecution execution = run(input, output);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(faultClassifier.classify(execution.getAllFailureExceptions().get(0)))
                .isEqualTo(FaultCategory.BUSINESS_ERROR);
        assertThat(output).doesNotExist();
    }

    @Test
    void emptyInputFailsAsBusinessError() throws Exception {
        Path input = writeInput("empty.dat", "");
        Path output = base.resolve("out").resolve("empty-out.dat");

        JobExecution execution = run(input, output);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(faultClassifier.classify(execution.getAllFailureExceptions().get(0)))
                .isEqualTo(FaultCategory.BUSINESS_ERROR);
    }

    @Test
    void invalidRecordFailsMovesInputToErrorAndWritesNoOutput() throws Exception {
        Path input = writeInput("bad.dat", "顧客001,1000\n顧客002,-5\n");
        Path output = base.resolve("out").resolve("bad-out.dat");

        JobExecution execution = run(input, output);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(faultClassifier.classify(execution.getAllFailureExceptions().get(0)))
                .isEqualTo(FaultCategory.BUSINESS_ERROR);
        // Atomic output: no final file appears on failure.
        assertThat(output).doesNotExist();
        // Input moved to the error directory.
        assertThat(input).doesNotExist();
        try (var errored = Files.list(base.resolve("error"))) {
            assertThat(errored.filter(p -> p.getFileName().toString().endsWith("bad.dat"))).hasSize(1);
        }
    }
}
