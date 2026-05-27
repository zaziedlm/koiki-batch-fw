package org.koikifw.libkoiki.batch.execution;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.koikifw.libkoiki.batch.fault.SystemException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;

class ConcurrencyGuardJobListenerTest {

    private final ConcurrencyGuardService guard = mock(ConcurrencyGuardService.class);

    private final ConcurrencyGuardJobListener listener = new ConcurrencyGuardJobListener(guard);

    private JobExecution execution() {
        JobExecution execution = mock(JobExecution.class);
        JobInstance instance = mock(JobInstance.class);
        lenient().when(instance.getJobName()).thenReturn("job");
        lenient().when(execution.getJobInstance()).thenReturn(instance);
        return execution;
    }

    @Test
    void proceedsWhenGuardAllows() {
        JobExecution execution = execution();
        when(guard.canRun(execution)).thenReturn(true);
        assertThatCode(() -> listener.beforeJob(execution)).doesNotThrowAnyException();
    }

    @Test
    void failsWhenGuardDenies() {
        JobExecution execution = execution();
        when(guard.canRun(execution)).thenReturn(false);
        assertThatThrownBy(() -> listener.beforeJob(execution))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("already running");
    }
}
