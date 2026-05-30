package org.koikifw.libkoiki.batch.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.koikifw.libkoiki.batch.fault.BusinessException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListener;

/**
 * Opt-in {@link JobExecutionListener} that manages the lifecycle of an ingestion
 * input file:
 *
 * <ul>
 *   <li>{@code beforeJob} — verifies the input file exists and is non-empty,
 *       failing fast with a {@link BusinessException} (exit code 20) otherwise.</li>
 *   <li>{@code afterJob} — archives the file when the job completes, or moves it
 *       to the error location otherwise, via the configured
 *       {@link FileArchivePolicy}.</li>
 * </ul>
 *
 * <p>The input file path is read from the job parameter
 * {@value #INPUT_FILE_PARAMETER}. When it is absent the listener does nothing, so
 * it composes cleanly with jobs that have no input file (e.g. DB&rarr;file): such
 * a job simply does not set the parameter. Jobs opt in by registering this
 * listener via {@code JobBuilder.listener(...)}.</p>
 */
public class FileIngestionLifecycleListener implements JobExecutionListener {

    /** Job parameter key holding the ingestion input file path. */
    public static final String INPUT_FILE_PARAMETER = "koiki.io.inputFile";

    private final FileArchivePolicy archivePolicy;

    public FileIngestionLifecycleListener(FileArchivePolicy archivePolicy) {
        this.archivePolicy = archivePolicy;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        Path file = inputFile(jobExecution);
        if (file == null) {
            return;
        }
        if (!Files.isRegularFile(file)) {
            throw new BusinessException("Ingestion input file not found: " + file);
        }
        try {
            if (Files.size(file) == 0L) {
                throw new BusinessException("Ingestion input file is empty: " + file);
            }
        } catch (IOException ex) {
            throw new BusinessException("Ingestion input file is not readable: " + file, ex);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Path file = inputFile(jobExecution);
        if (file == null) {
            return;
        }
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            archivePolicy.archive(file);
        } else {
            archivePolicy.moveToError(file);
        }
    }

    private static Path inputFile(JobExecution jobExecution) {
        JobParameters parameters = jobExecution.getJobParameters();
        String inputFile = parameters.getString(INPUT_FILE_PARAMETER);
        return (inputFile == null || inputFile.isBlank()) ? null : Path.of(inputFile);
    }
}
