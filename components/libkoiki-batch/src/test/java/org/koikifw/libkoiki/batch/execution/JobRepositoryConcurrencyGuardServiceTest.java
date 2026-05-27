package org.koikifw.libkoiki.batch.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.repository.JobRepository;

class JobRepositoryConcurrencyGuardServiceTest {

    private final JobRepository jobRepository = mock(JobRepository.class);

    private final ConcurrencyGuardService guard = new JobRepositoryConcurrencyGuardService(jobRepository);

    @Test
    void acquiresWhenNothingRunning() {
        when(jobRepository.findRunningJobExecutions("job")).thenReturn(Set.of());
        assertThat(guard.acquire("job")).isTrue();
    }

    @Test
    void deniesWhenExecutionRunning() {
        when(jobRepository.findRunningJobExecutions("job")).thenReturn(Set.of(mock(JobExecution.class)));
        assertThat(guard.acquire("job")).isFalse();
    }
}
