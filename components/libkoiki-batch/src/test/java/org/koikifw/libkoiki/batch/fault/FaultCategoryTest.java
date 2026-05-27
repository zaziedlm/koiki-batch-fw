package org.koikifw.libkoiki.batch.fault;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Locks the exit code mapping to {@code ops/jp1/jobs/return-code-mapping.md}.
 */
class FaultCategoryTest {

    @Test
    void exitCodesMatchReturnCodeMapping() {
        assertThat(FaultCategory.NORMAL.exitCode()).isEqualTo(0);
        assertThat(FaultCategory.WARNING.exitCode()).isEqualTo(10);
        assertThat(FaultCategory.BUSINESS_ERROR.exitCode()).isEqualTo(20);
        assertThat(FaultCategory.SYSTEM_ERROR.exitCode()).isEqualTo(30);
    }
}
