package org.koikifw.refapp.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.koikifw.libkoiki.batch.core.KoikiBatchProperties;
import org.koikifw.libkoiki.batch.execution.ConcurrencyGuardJobListener;
import org.koikifw.libkoiki.batch.execution.ConcurrencyGuardService;
import org.koikifw.libkoiki.batch.execution.KoikiJobParametersValidator;
import org.koikifw.libkoiki.batch.fault.FaultClassifier;
import org.koikifw.libkoiki.batch.fault.KoikiBatchExitCodeGenerator;
import org.koikifw.libkoiki.batch.fault.KoikiExitCodeExceptionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Confirms that libkoiki-batch auto-configuration activates in a downstream
 * Spring Boot application and that all Phase 0 framework beans are present.
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
class BatchCoreWiringIT {

    @Autowired
    private KoikiBatchProperties properties;

    @Autowired
    private KoikiJobParametersValidator jobParametersValidator;

    @Autowired
    private FaultClassifier faultClassifier;

    @Autowired
    private KoikiBatchExitCodeGenerator exitCodeGenerator;

    @Autowired
    private KoikiExitCodeExceptionMapper exitCodeExceptionMapper;

    @Autowired
    private ConcurrencyGuardService concurrencyGuardService;

    @Autowired
    private ConcurrencyGuardJobListener concurrencyGuardJobListener;

    @Test
    void frameworkBeansAreWired() {
        assertThat(properties).isNotNull();
        assertThat(jobParametersValidator).isNotNull();
        assertThat(faultClassifier).isNotNull();
        assertThat(exitCodeGenerator).isNotNull();
        assertThat(exitCodeExceptionMapper).isNotNull();
        assertThat(concurrencyGuardService).isNotNull();
        assertThat(concurrencyGuardJobListener).isNotNull();
    }
}
