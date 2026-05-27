package org.koikifw.libkoiki.batch.execution;

import org.springframework.batch.core.job.JobExecution;

/**
 * Guards against concurrent execution of the same job.
 *
 * <p>The v0.1.0 contract is single-process detection of running executions.
 * Distributed locking is intentionally out of scope.</p>
 */
public interface ConcurrencyGuardService {

    /**
     * Whether the given execution is allowed to run.
     *
     * @param execution the execution about to start
     * @return {@code true} if no <em>other</em> execution of the same job is
     *         currently running (the given execution is excluded from the check)
     */
    boolean canRun(JobExecution execution);
}
