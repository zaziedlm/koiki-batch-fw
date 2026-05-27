package org.koikifw.libkoiki.batch.execution;

import org.koikifw.libkoiki.batch.fault.SystemException;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

/**
 * Enforces the {@link ConcurrencyGuardService} before a job runs.
 *
 * <p>Jobs opt in by registering this listener. If another execution of the same
 * job is already running, the job fails with a {@link SystemException}, which
 * maps to exit code {@code 30}.</p>
 */
public class ConcurrencyGuardJobListener implements JobExecutionListener {

    private final ConcurrencyGuardService concurrencyGuardService;

    public ConcurrencyGuardJobListener(ConcurrencyGuardService concurrencyGuardService) {
        this.concurrencyGuardService = concurrencyGuardService;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        if (!concurrencyGuardService.canRun(jobExecution)) {
            throw new SystemException("Another execution of job '"
                    + jobExecution.getJobInstance().getJobName() + "' is already running");
        }
    }
}
