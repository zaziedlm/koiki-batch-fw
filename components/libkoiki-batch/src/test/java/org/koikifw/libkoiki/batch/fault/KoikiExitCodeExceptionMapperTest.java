package org.koikifw.libkoiki.batch.fault;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;

class KoikiExitCodeExceptionMapperTest {

    private final KoikiExitCodeExceptionMapper mapper =
            new KoikiExitCodeExceptionMapper(new DefaultFaultClassifier());

    @Test
    void mapsInvalidJobParametersToBusinessError() {
        assertThat(mapper.getExitCode(new InvalidJobParametersException("missing requestId"))).isEqualTo(20);
    }

    @Test
    void mapsWrappedInvalidJobParametersToBusinessError() {
        assertThat(mapper.getExitCode(new RuntimeException(new InvalidJobParametersException("bad")))).isEqualTo(20);
    }

    @Test
    void mapsBusinessExceptionToTwenty() {
        assertThat(mapper.getExitCode(new BusinessException("rule"))).isEqualTo(20);
    }

    @Test
    void mapsSystemExceptionToThirty() {
        assertThat(mapper.getExitCode(new SystemException("infra"))).isEqualTo(30);
    }

    @Test
    void mapsUnknownExceptionToSystemError() {
        assertThat(mapper.getExitCode(new IllegalStateException("boom"))).isEqualTo(30);
    }
}
