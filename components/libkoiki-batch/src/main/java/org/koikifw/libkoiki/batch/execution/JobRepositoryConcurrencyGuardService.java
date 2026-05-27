package org.koikifw.libkoiki.batch.execution;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.repository.JobRepository;

/**
 * Default {@link ConcurrencyGuardService} backed by {@link JobRepository}.
 *
 * <p>In Spring Batch 6 {@code JobRepository} absorbs the former {@code JobExplorer}
 * query API, so running executions are inspected through it; the deprecated
 * {@code JobExplorer} bean and {@code JobOperator#getRunningExecutions} are not used.</p>
 */
public class JobRepositoryConcurrencyGuardService implements ConcurrencyGuardService {

    private final JobRepository jobRepository;

    public JobRepositoryConcurrencyGuardService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public boolean canRun(JobExecution execution) {
        String jobName = execution.getJobInstance().getJobName();
        long currentId = execution.getId();
        return jobRepository.findRunningJobExecutions(jobName).stream()
                .allMatch(running -> running.getId() == currentId);
    }
}
