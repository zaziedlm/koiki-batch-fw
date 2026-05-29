package org.koikifw.libkoiki.batch.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.koikifw.libkoiki.batch.execution.ConcurrencyGuardService;
import org.koikifw.libkoiki.batch.execution.KoikiJobParametersValidator;
import org.koikifw.libkoiki.batch.fault.FaultClassifier;
import org.koikifw.libkoiki.batch.fault.KoikiBatchExitCodeGenerator;
import org.koikifw.libkoiki.batch.observability.JobLogListener;
import org.koikifw.libkoiki.batch.observability.StepLogListener;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class BatchCoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BatchCoreAutoConfiguration.class));

    @Test
    void registersFrameworkBeansByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(KoikiBatchProperties.class);
            assertThat(context).hasSingleBean(FaultClassifier.class);
            assertThat(context).hasSingleBean(KoikiJobParametersValidator.class);
            assertThat(context).hasSingleBean(KoikiBatchExitCodeGenerator.class);
            assertThat(context).hasSingleBean(JobLogListener.class);
            assertThat(context).hasSingleBean(StepLogListener.class);
        });
    }

    @Test
    void loggingListenersCanBeDisabled() {
        runner.withPropertyValues("koiki.batch.logging.correlation.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JobLogListener.class);
                    assertThat(context).doesNotHaveBean(StepLogListener.class);
                });
    }

    @Test
    void doesNotCreateConcurrencyGuardWithoutJobRepository() {
        runner.run(context -> assertThat(context).doesNotHaveBean(ConcurrencyGuardService.class));
    }

    @Test
    void createsConcurrencyGuardWhenJobRepositoryPresent() {
        runner.withBean("jobRepository", JobRepository.class, () -> mock(JobRepository.class))
                .run(context -> assertThat(context).hasSingleBean(ConcurrencyGuardService.class));
    }

    @Test
    void exitCodeGeneratorCanBeDisabled() {
        runner.withPropertyValues("koiki.batch.exit-code.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(KoikiBatchExitCodeGenerator.class));
    }
}
