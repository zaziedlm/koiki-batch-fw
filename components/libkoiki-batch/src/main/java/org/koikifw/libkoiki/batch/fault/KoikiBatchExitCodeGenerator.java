package org.koikifw.libkoiki.batch.fault;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.batch.autoconfigure.JobExecutionEvent;
import org.springframework.context.ApplicationListener;

/**
 * Maps batch outcomes to the KOIKI process exit codes {@code 0/10/20/30}
 * (see {@code ops/jp1/jobs/return-code-mapping.md}).
 *
 * <p>This is the Spring Boot launch path: it listens to {@link JobExecutionEvent}
 * published by {@code JobLauncherApplicationRunner} and exposes the resulting
 * process exit code via {@link ExitCodeGenerator}, the same mechanism Boot's own
 * {@code JobExecutionExitCodeGenerator} uses.</p>
 */
public class KoikiBatchExitCodeGenerator
        implements ApplicationListener<JobExecutionEvent>, ExitCodeGenerator {

    private final FaultClassifier faultClassifier;

    private final List<JobExecution> executions = new CopyOnWriteArrayList<>();

    public KoikiBatchExitCodeGenerator(FaultClassifier faultClassifier) {
        this.faultClassifier = faultClassifier;
    }

    @Override
    public void onApplicationEvent(JobExecutionEvent event) {
        executions.add(event.getJobExecution());
    }

    @Override
    public int getExitCode() {
        int exitCode = FaultCategory.NORMAL.exitCode();
        for (JobExecution execution : executions) {
            exitCode = Math.max(exitCode,
                    resolve(execution.getStatus(), execution.getAllFailureExceptions()).exitCode());
        }
        return exitCode;
    }

    /**
     * Resolves a single execution outcome to a {@link FaultCategory}.
     */
    public FaultCategory resolve(BatchStatus status, List<Throwable> failures) {
        if (status == BatchStatus.COMPLETED) {
            return FaultCategory.NORMAL;
        }
        FaultCategory category = FaultCategory.SYSTEM_ERROR;
        boolean classified = false;
        if (failures != null) {
            for (Throwable failure : failures) {
                FaultCategory candidate = faultClassifier.classify(failure);
                if (!classified || candidate.exitCode() > category.exitCode()) {
                    category = candidate;
                    classified = true;
                }
            }
        }
        return category;
    }
}
