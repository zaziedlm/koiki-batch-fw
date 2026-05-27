package org.koikifw.libkoiki.batch.fault;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DefaultFaultClassifierTest {

    private final FaultClassifier classifier = new DefaultFaultClassifier();

    @Test
    void classifiesBusinessException() {
        assertThat(classifier.classify(new BusinessException("bad input")))
                .isEqualTo(FaultCategory.BUSINESS_ERROR);
    }

    @Test
    void classifiesSystemException() {
        assertThat(classifier.classify(new SystemException("infra down")))
                .isEqualTo(FaultCategory.SYSTEM_ERROR);
    }

    @Test
    void unwrapsCauseChain() {
        Throwable wrapped = new RuntimeException("step failed", new BusinessException("rule violated"));
        assertThat(classifier.classify(wrapped)).isEqualTo(FaultCategory.BUSINESS_ERROR);
    }

    @Test
    void defaultsToSystemErrorForUnknown() {
        assertThat(classifier.classify(new IllegalStateException("boom")))
                .isEqualTo(FaultCategory.SYSTEM_ERROR);
    }

    @Test
    void treatsNullAsNormal() {
        assertThat(classifier.classify(null)).isEqualTo(FaultCategory.NORMAL);
    }
}
