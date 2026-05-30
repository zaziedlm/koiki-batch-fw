package org.koikifw.libkoiki.batch.io;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.koikifw.libkoiki.batch.fault.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

class FileIngestionLifecycleListenerTest {

    @TempDir
    Path tmp;

    private final FileArchivePolicy policy = mock(FileArchivePolicy.class);

    private final FileIngestionLifecycleListener listener = new FileIngestionLifecycleListener(policy);

    private JobExecution execution(BatchStatus status, String inputFile) {
        JobParameters params = inputFile == null
                ? new JobParameters()
                : new JobParametersBuilder()
                        .addString(FileIngestionLifecycleListener.INPUT_FILE_PARAMETER, inputFile)
                        .toJobParameters();
        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(status);
        when(execution.getJobParameters()).thenReturn(params);
        return execution;
    }

    @Test
    void beforeJobPassesForExistingNonEmptyFile() throws IOException {
        Path file = tmp.resolve("in.dat");
        Files.writeString(file, "data");

        assertThatCode(() -> listener.beforeJob(execution(BatchStatus.STARTING, file.toString())))
                .doesNotThrowAnyException();
    }

    @Test
    void beforeJobFailsForMissingFile() {
        Path file = tmp.resolve("missing.dat");

        assertThatThrownBy(() -> listener.beforeJob(execution(BatchStatus.STARTING, file.toString())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void beforeJobFailsForEmptyFile() throws IOException {
        Path file = tmp.resolve("empty.dat");
        Files.createFile(file);

        assertThatThrownBy(() -> listener.beforeJob(execution(BatchStatus.STARTING, file.toString())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void beforeJobNoOpWithoutInputParameter() {
        assertThatCode(() -> listener.beforeJob(execution(BatchStatus.STARTING, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void afterJobArchivesOnCompletion() {
        listener.afterJob(execution(BatchStatus.COMPLETED, "/data/in.dat"));

        verify(policy).archive(Path.of("/data/in.dat"));
        verify(policy, never()).moveToError(any());
    }

    @Test
    void afterJobMovesToErrorOnFailure() {
        listener.afterJob(execution(BatchStatus.FAILED, "/data/in.dat"));

        verify(policy).moveToError(Path.of("/data/in.dat"));
        verify(policy, never()).archive(any());
    }

    @Test
    void afterJobNoOpWithoutInputParameter() {
        listener.afterJob(execution(BatchStatus.COMPLETED, null));

        verify(policy, never()).archive(any());
        verify(policy, never()).moveToError(any());
    }
}
