package org.koikifw.libkoiki.batch.execution;

/**
 * Guards against concurrent execution of the same job.
 *
 * <p>The v0.1.0 contract is single-process detection of running executions.
 * Distributed locking is intentionally out of scope.</p>
 */
public interface ConcurrencyGuardService {

    /**
     * Attempts to acquire the right to run the given job.
     *
     * @param jobName the job name to guard
     * @return {@code true} if no execution of the job is currently running
     */
    boolean acquire(String jobName);
}
