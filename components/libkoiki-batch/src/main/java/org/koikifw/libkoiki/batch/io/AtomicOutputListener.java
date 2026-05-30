package org.koikifw.libkoiki.batch.io;

import java.nio.file.Path;

import org.koikifw.libkoiki.batch.fault.SystemException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListener;

/**
 * Opt-in {@link JobExecutionListener} that finalizes atomic file output: when the
 * job completes it promotes the in-progress temp file to the final path,
 * otherwise it discards the temp file.
 *
 * <p>The final output path is read from the job parameter
 * {@value #OUTPUT_FILE_PARAMETER}; the temp path is derived with
 * {@link AtomicFileOutput#tempPath(Path, String)} using the configured suffix
 * (the job's writer must write to that same temp path). When the parameter is
 * absent the listener does nothing, so it composes cleanly with jobs that have no
 * file output (e.g. file&rarr;DB): such a job simply does not set the parameter.</p>
 */
public class AtomicOutputListener implements JobExecutionListener {

    /** Job parameter key holding the final output file path. */
    public static final String OUTPUT_FILE_PARAMETER = "koiki.io.outputFile";

    private final String tempSuffix;

    public AtomicOutputListener(String tempSuffix) {
        this.tempSuffix = tempSuffix;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        JobParameters parameters = jobExecution.getJobParameters();
        String outputFile = parameters.getString(OUTPUT_FILE_PARAMETER);
        if (outputFile == null || outputFile.isBlank()) {
            return;
        }
        Path finalPath = Path.of(outputFile);
        Path temp = AtomicFileOutput.tempPath(finalPath, tempSuffix);
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            try {
                AtomicFileOutput.promote(temp, finalPath);
            } catch (SystemException ex) {
                jobExecution.setStatus(BatchStatus.FAILED);
                jobExecution.addFailureException(ex);
                throw ex;
            }
        } else {
            AtomicFileOutput.discard(temp);
        }
    }
}
