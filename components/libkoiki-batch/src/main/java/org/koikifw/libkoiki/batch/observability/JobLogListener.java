package org.koikifw.libkoiki.batch.observability;

import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListener;

/**
 * Populates the SLF4J MDC with KOIKI job correlation keys around job execution
 * and emits start/end log lines for operational diagnosis.
 *
 * <p>Optional keys ({@code bizDate}, {@code requestId}, {@code tenant}) are only
 * written when the corresponding job parameter is present. MDC entries are
 * always cleared in {@code afterJob}, even if logging itself fails.</p>
 */
public class JobLogListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobLogListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        MDC.put(CorrelationKeys.JOB_NAME, jobExecution.getJobInstance().getJobName());
        MDC.put(CorrelationKeys.JOB_EXEC_ID, String.valueOf(jobExecution.getId()));
        JobParameters parameters = jobExecution.getJobParameters();
        putIfPresent(CorrelationKeys.JOB_BIZ_DATE, parameters.getString(StandardJobParameters.BIZ_DATE));
        putIfPresent(CorrelationKeys.JOB_REQUEST_ID, parameters.getString(StandardJobParameters.REQUEST_ID));
        putIfPresent(CorrelationKeys.JOB_TENANT, parameters.getString(CorrelationKeys.TENANT_PARAMETER));
        log.info("Job started");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        try {
            log.info("Job ended: status={}, exitCode={}",
                    jobExecution.getStatus(),
                    jobExecution.getExitStatus().getExitCode());
        } finally {
            MDC.remove(CorrelationKeys.JOB_TENANT);
            MDC.remove(CorrelationKeys.JOB_REQUEST_ID);
            MDC.remove(CorrelationKeys.JOB_BIZ_DATE);
            MDC.remove(CorrelationKeys.JOB_EXEC_ID);
            MDC.remove(CorrelationKeys.JOB_NAME);
        }
    }

    private static void putIfPresent(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }
}
