package org.koikifw.libkoiki.batch.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.koikifw.libkoiki.batch.execution.ConcurrencyGuardService;
import org.koikifw.libkoiki.batch.execution.KoikiJobParametersValidator;
import org.koikifw.libkoiki.batch.audit.AuditEventPublisher;
import org.koikifw.libkoiki.batch.audit.LoggingAuditEventPublisher;
import org.koikifw.libkoiki.batch.fault.FaultClassifier;
import org.koikifw.libkoiki.batch.fault.KoikiBatchExitCodeGenerator;
import org.koikifw.libkoiki.batch.io.AtomicOutputListener;
import org.koikifw.libkoiki.batch.io.FileArchivePolicy;
import org.koikifw.libkoiki.batch.io.FileIngestionLifecycleListener;
import org.koikifw.libkoiki.batch.observability.JobLogListener;
import org.koikifw.libkoiki.batch.observability.StepLogListener;
import org.koikifw.libkoiki.batch.security.Masker;
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
            assertThat(context).hasSingleBean(AuditEventPublisher.class);
            assertThat(context.getBean(AuditEventPublisher.class))
                    .isInstanceOf(LoggingAuditEventPublisher.class);
            assertThat(context).hasSingleBean(Masker.class);
            assertThat(context).hasSingleBean(FileArchivePolicy.class);
            assertThat(context).hasSingleBean(FileIngestionLifecycleListener.class);
            assertThat(context).hasSingleBean(AtomicOutputListener.class);
        });
    }

    @Test
    void ioFileCharsetDefaultsToMs932AndBinds() {
        runner.run(context ->
                assertThat(context.getBean(KoikiBatchProperties.class).getIo().getFile().getCharset())
                        .isEqualTo("MS932"));
        runner.withPropertyValues("koiki.batch.io.file.charset=UTF-8")
                .run(context -> assertThat(context.getBean(KoikiBatchProperties.class)
                        .getIo().getFile().getCharset()).isEqualTo("UTF-8"));
    }

    @Test
    void transactionDefaultCommitIntervalDefaultsTo100() {
        runner.run(context ->
                assertThat(context.getBean(KoikiBatchProperties.class)
                        .getTransaction().getDefaultCommitInterval()).isEqualTo(100));
    }

    @Test
    void transactionDefaultCommitIntervalIsBindable() {
        runner.withPropertyValues("koiki.batch.transaction.default-commit-interval=500")
                .run(context -> assertThat(context.getBean(KoikiBatchProperties.class)
                        .getTransaction().getDefaultCommitInterval()).isEqualTo(500));
    }

    @Test
    void maskerCanBeDisabledWhileAuditStaysEnabled() {
        runner.withPropertyValues("koiki.batch.security.masking.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(Masker.class);
                    // Audit publishing still works; it simply passes values through unmasked.
                    assertThat(context).hasSingleBean(AuditEventPublisher.class);
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
    void auditPublisherCanBeDisabled() {
        runner.withPropertyValues("koiki.batch.audit.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AuditEventPublisher.class));
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
