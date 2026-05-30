package org.koikifw.libkoiki.batch.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

class AtomicOutputTest {

    @TempDir
    Path tmp;

    // --- AtomicFileOutput ---

    @Test
    void tempPathAppendsSuffix() {
        Path finalPath = Path.of("/out/result.csv");
        assertThat(AtomicFileOutput.tempPath(finalPath, ".inprogress"))
                .isEqualTo(Path.of("/out/result.csv.inprogress"));
    }

    @Test
    void promoteMovesTempToFinal() throws IOException {
        Path finalPath = tmp.resolve("result.csv");
        Path temp = AtomicFileOutput.tempPath(finalPath, ".inprogress");
        Files.writeString(temp, "payload");

        AtomicFileOutput.promote(temp, finalPath);

        assertThat(temp).doesNotExist();
        assertThat(finalPath).exists().content().isEqualTo("payload");
    }

    @Test
    void promoteWithMissingTempIsNoOp() {
        Path finalPath = tmp.resolve("result.csv");
        Path temp = AtomicFileOutput.tempPath(finalPath, ".inprogress");

        assertThatCode(() -> AtomicFileOutput.promote(temp, finalPath)).doesNotThrowAnyException();
        assertThat(finalPath).doesNotExist();
    }

    @Test
    void discardDeletesTemp() throws IOException {
        Path finalPath = tmp.resolve("result.csv");
        Path temp = AtomicFileOutput.tempPath(finalPath, ".inprogress");
        Files.writeString(temp, "payload");

        AtomicFileOutput.discard(temp);

        assertThat(temp).doesNotExist();
    }

    // --- AtomicOutputListener ---

    private JobExecution execution(BatchStatus status, String outputFile) {
        JobParameters params = outputFile == null
                ? new JobParameters()
                : new JobParametersBuilder()
                        .addString(AtomicOutputListener.OUTPUT_FILE_PARAMETER, outputFile)
                        .toJobParameters();
        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(status);
        when(execution.getJobParameters()).thenReturn(params);
        return execution;
    }

    @Test
    void listenerPromotesOnCompletion() throws IOException {
        Path finalPath = tmp.resolve("out.csv");
        Path temp = AtomicFileOutput.tempPath(finalPath, ".inprogress");
        Files.writeString(temp, "payload");

        new AtomicOutputListener(".inprogress").afterJob(execution(BatchStatus.COMPLETED, finalPath.toString()));

        assertThat(temp).doesNotExist();
        assertThat(finalPath).exists().content().isEqualTo("payload");
    }

    @Test
    void listenerDiscardsOnFailure() throws IOException {
        Path finalPath = tmp.resolve("out.csv");
        Path temp = AtomicFileOutput.tempPath(finalPath, ".inprogress");
        Files.writeString(temp, "payload");

        new AtomicOutputListener(".inprogress").afterJob(execution(BatchStatus.FAILED, finalPath.toString()));

        assertThat(temp).doesNotExist();
        assertThat(finalPath).doesNotExist();
    }

    @Test
    void listenerNoOpWithoutOutputParameter() {
        assertThatCode(() -> new AtomicOutputListener(".inprogress")
                .afterJob(execution(BatchStatus.COMPLETED, null))).doesNotThrowAnyException();
    }
}
