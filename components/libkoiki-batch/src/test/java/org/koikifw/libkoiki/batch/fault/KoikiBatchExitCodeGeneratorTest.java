package org.koikifw.libkoiki.batch.fault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.boot.batch.autoconfigure.JobExecutionEvent;

class KoikiBatchExitCodeGeneratorTest {

    private final KoikiBatchExitCodeGenerator generator =
            new KoikiBatchExitCodeGenerator(new DefaultFaultClassifier());

    @Test
    void completedMapsToNormal() {
        assertThat(generator.resolve(BatchStatus.COMPLETED, List.of())).isEqualTo(FaultCategory.NORMAL);
    }

    @Test
    void businessFailureMapsToBusinessError() {
        assertThat(generator.resolve(BatchStatus.FAILED, List.of(new BusinessException("rule"))))
                .isEqualTo(FaultCategory.BUSINESS_ERROR);
    }

    @Test
    void systemFailureMapsToSystemError() {
        assertThat(generator.resolve(BatchStatus.FAILED, List.of(new SystemException("infra"))))
                .isEqualTo(FaultCategory.SYSTEM_ERROR);
    }

    @Test
    void failedWithoutExceptionsMapsToSystemError() {
        assertThat(generator.resolve(BatchStatus.FAILED, List.of())).isEqualTo(FaultCategory.SYSTEM_ERROR);
    }

    @Test
    void takesMostSevereCategory() {
        assertThat(generator.resolve(BatchStatus.FAILED,
                List.of(new BusinessException("b"), new SystemException("s"))))
                .isEqualTo(FaultCategory.SYSTEM_ERROR);
    }

    @Test
    void noEventsMeansZeroExitCode() {
        assertThat(generator.getExitCode()).isEqualTo(0);
    }

    @Test
    void stoppedMapsToSystemError() {
        assertThat(generator.resolve(BatchStatus.STOPPED, List.of())).isEqualTo(FaultCategory.SYSTEM_ERROR);
    }

    @Test
    void abandonedMapsToSystemError() {
        assertThat(generator.resolve(BatchStatus.ABANDONED, List.of())).isEqualTo(FaultCategory.SYSTEM_ERROR);
    }

    @Test
    void unknownMapsToSystemError() {
        assertThat(generator.resolve(BatchStatus.UNKNOWN, List.of())).isEqualTo(FaultCategory.SYSTEM_ERROR);
    }

    @Test
    void aggregatesAcrossMultipleEventsTakingHighestExitCode() {
        generator.onApplicationEvent(eventOf(BatchStatus.COMPLETED, List.of()));
        generator.onApplicationEvent(eventOf(BatchStatus.FAILED, List.of(new BusinessException("biz"))));
        generator.onApplicationEvent(eventOf(BatchStatus.FAILED, List.of(new SystemException("sys"))));

        assertThat(generator.getExitCode()).isEqualTo(FaultCategory.SYSTEM_ERROR.exitCode());
    }

    @Test
    void aggregatesBusinessOverSuccess() {
        generator.onApplicationEvent(eventOf(BatchStatus.COMPLETED, List.of()));
        generator.onApplicationEvent(eventOf(BatchStatus.FAILED, List.of(new BusinessException("biz"))));

        assertThat(generator.getExitCode()).isEqualTo(FaultCategory.BUSINESS_ERROR.exitCode());
    }

    private JobExecutionEvent eventOf(BatchStatus status, List<Throwable> failures) {
        JobExecution execution = mock(JobExecution.class);
        lenient().when(execution.getStatus()).thenReturn(status);
        lenient().when(execution.getAllFailureExceptions()).thenReturn(failures);
        JobExecutionEvent event = mock(JobExecutionEvent.class);
        when(event.getJobExecution()).thenReturn(execution);
        return event;
    }
}
