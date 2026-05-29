package org.koikifw.libkoiki.batch.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepExecution;

class StepLogListenerTest {

    private final StepLogListener listener = new StepLogListener();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private StepExecution stepExecution() {
        StepExecution stepExecution = mock(StepExecution.class);
        lenient().when(stepExecution.getStepName()).thenReturn("customer-daily-sync-step");
        lenient().when(stepExecution.getId()).thenReturn(7L);
        lenient().when(stepExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        lenient().when(stepExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        lenient().when(stepExecution.getReadCount()).thenReturn(0L);
        lenient().when(stepExecution.getWriteCount()).thenReturn(0L);
        return stepExecution;
    }

    @Test
    void beforeStepPutsStepMdcKeys() {
        listener.beforeStep(stepExecution());

        assertThat(MDC.get(CorrelationKeys.STEP_NAME)).isEqualTo("customer-daily-sync-step");
        assertThat(MDC.get(CorrelationKeys.STEP_EXEC_ID)).isEqualTo("7");
    }

    @Test
    void afterStepClearsMdcAndReturnsNull() {
        StepExecution execution = stepExecution();
        listener.beforeStep(execution);

        ExitStatus result = listener.afterStep(execution);

        assertThat(result).isNull();
        assertThat(MDC.get(CorrelationKeys.STEP_NAME)).isNull();
        assertThat(MDC.get(CorrelationKeys.STEP_EXEC_ID)).isNull();
    }

    @Test
    void afterStepClearsMdcEvenWhenLoggingThrows() {
        StepExecution execution = mock(StepExecution.class);
        lenient().when(execution.getStepName()).thenReturn("customer-daily-sync-step");
        lenient().when(execution.getId()).thenReturn(7L);
        when(execution.getStatus()).thenThrow(new RuntimeException("logging boom"));
        listener.beforeStep(execution);

        assertThatThrownBy(() -> listener.afterStep(execution))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("logging boom");

        assertThat(MDC.get(CorrelationKeys.STEP_NAME)).isNull();
        assertThat(MDC.get(CorrelationKeys.STEP_EXEC_ID)).isNull();
    }
}
