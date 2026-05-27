package org.koikifw.libkoiki.batch.fault;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;

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
}
