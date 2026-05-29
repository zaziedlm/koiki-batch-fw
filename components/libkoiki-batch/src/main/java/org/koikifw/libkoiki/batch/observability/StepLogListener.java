package org.koikifw.libkoiki.batch.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;

/**
 * Adds step-scoped MDC keys around step execution and emits start/end log
 * lines. Returns {@code null} from {@code afterStep} to keep Spring Batch's
 * computed exit status.
 */
public class StepLogListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(StepLogListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        MDC.put(CorrelationKeys.STEP_NAME, stepExecution.getStepName());
        MDC.put(CorrelationKeys.STEP_EXEC_ID, String.valueOf(stepExecution.getId()));
        log.info("Step started");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        try {
            log.info("Step ended: status={}, read={}, write={}",
                    stepExecution.getStatus(),
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount());
            return null;
        } finally {
            MDC.remove(CorrelationKeys.STEP_EXEC_ID);
            MDC.remove(CorrelationKeys.STEP_NAME);
        }
    }
}
