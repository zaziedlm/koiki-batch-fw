package org.koikifw.libkoiki.batch.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.repository.JobRepository;

class JobRepositoryConcurrencyGuardServiceTest {

    private final JobRepository jobRepository = mock(JobRepository.class);

    private final ConcurrencyGuardService guard = new JobRepositoryConcurrencyGuardService(jobRepository);

    private JobExecution execution(long id) {
        JobExecution execution = mock(JobExecution.class);
        JobInstance instance = mock(JobInstance.class);
        lenient().when(instance.getJobName()).thenReturn("job");
        lenient().when(execution.getJobInstance()).thenReturn(instance);
        lenient().when(execution.getId()).thenReturn(id);
        return execution;
    }

    @Test
    void allowsWhenOnlySelfIsRunning() {
        JobExecution current = execution(1L);
        when(jobRepository.findRunningJobExecutions("job")).thenReturn(Set.of(current));
        assertThat(guard.canRun(current)).isTrue();
    }

    @Test
    void allowsWhenNothingIsRunning() {
        JobExecution current = execution(1L);
        when(jobRepository.findRunningJobExecutions("job")).thenReturn(Set.of());
        assertThat(guard.canRun(current)).isTrue();
    }

    @Test
    void deniesWhenAnotherExecutionIsRunning() {
        JobExecution current = execution(1L);
        JobExecution other = execution(2L);
        when(jobRepository.findRunningJobExecutions("job")).thenReturn(Set.of(current, other));
        assertThat(guard.canRun(current)).isFalse();
    }
}
