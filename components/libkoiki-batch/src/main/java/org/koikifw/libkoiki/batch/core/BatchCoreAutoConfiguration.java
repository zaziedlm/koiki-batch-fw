package org.koikifw.libkoiki.batch.core;

import org.koikifw.libkoiki.batch.audit.AuditEventPublisher;
import org.koikifw.libkoiki.batch.audit.LoggingAuditEventPublisher;
import org.koikifw.libkoiki.batch.execution.ConcurrencyGuardJobListener;
import org.koikifw.libkoiki.batch.execution.ConcurrencyGuardService;
import org.koikifw.libkoiki.batch.execution.JobRepositoryConcurrencyGuardService;
import org.koikifw.libkoiki.batch.execution.KoikiJobParametersValidator;
import org.koikifw.libkoiki.batch.fault.DefaultFaultClassifier;
import org.koikifw.libkoiki.batch.fault.FaultClassifier;
import org.koikifw.libkoiki.batch.fault.KoikiBatchExitCodeGenerator;
import org.koikifw.libkoiki.batch.fault.KoikiExitCodeExceptionMapper;
import org.koikifw.libkoiki.batch.io.AtomicOutputListener;
import org.koikifw.libkoiki.batch.io.DirectoryFileArchivePolicy;
import org.koikifw.libkoiki.batch.io.FileArchivePolicy;
import org.koikifw.libkoiki.batch.io.FileIngestionLifecycleListener;
import org.koikifw.libkoiki.batch.observability.JobLogListener;
import org.koikifw.libkoiki.batch.observability.StepLogListener;
import org.koikifw.libkoiki.batch.security.Masker;
import org.koikifw.libkoiki.batch.security.RedactingMasker;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.JobExecutionEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration entry point for the KOIKI batch framework.
 *
 * <p>Runs after Spring Boot's {@link BatchAutoConfiguration} and only adds
 * framework beans; it never redefines Boot-provided batch infrastructure
 * (such as {@code JobRepository} or {@code JobOperator}).</p>
 */
@AutoConfiguration(after = BatchAutoConfiguration.class)
@EnableConfigurationProperties(KoikiBatchProperties.class)
public class BatchCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KoikiJobParametersValidator koikiJobParametersValidator() {
        return new KoikiJobParametersValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public FaultClassifier faultClassifier() {
        return new DefaultFaultClassifier();
    }

    @Bean
    @ConditionalOnClass(JobExecutionEvent.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "koiki.batch.exit-code", name = "enabled", matchIfMissing = true)
    public KoikiBatchExitCodeGenerator koikiBatchExitCodeGenerator(FaultClassifier faultClassifier) {
        return new KoikiBatchExitCodeGenerator(faultClassifier);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "koiki.batch.exit-code", name = "enabled", matchIfMissing = true)
    public KoikiExitCodeExceptionMapper koikiExitCodeExceptionMapper(FaultClassifier faultClassifier) {
        return new KoikiExitCodeExceptionMapper(faultClassifier);
    }

    @Bean
    @ConditionalOnBean(JobRepository.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "koiki.batch.concurrency-guard", name = "enabled", matchIfMissing = true)
    public ConcurrencyGuardService concurrencyGuardService(JobRepository jobRepository) {
        return new JobRepositoryConcurrencyGuardService(jobRepository);
    }

    @Bean
    @ConditionalOnBean(ConcurrencyGuardService.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "koiki.batch.concurrency-guard", name = "enabled", matchIfMissing = true)
    public ConcurrencyGuardJobListener concurrencyGuardJobListener(ConcurrencyGuardService concurrencyGuardService) {
        return new ConcurrencyGuardJobListener(concurrencyGuardService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "koiki.batch.logging.correlation", name = "enabled", matchIfMissing = true)
    public JobLogListener jobLogListener() {
        return new JobLogListener();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "koiki.batch.logging.correlation", name = "enabled", matchIfMissing = true)
    public StepLogListener stepLogListener() {
        return new StepLogListener();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "koiki.batch.security.masking", name = "enabled", matchIfMissing = true)
    public Masker masker(KoikiBatchProperties properties) {
        return new RedactingMasker(properties.getSecurity().getMasking().getMask());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "koiki.batch.audit", name = "enabled", matchIfMissing = true)
    public AuditEventPublisher auditEventPublisher(KoikiBatchProperties properties, ObjectProvider<Masker> masker) {
        return new LoggingAuditEventPublisher(
                masker.getIfAvailable(),
                properties.getSecurity().getMasking().getSensitiveKeys());
    }

    @Bean
    @ConditionalOnMissingBean
    public FileArchivePolicy fileArchivePolicy(KoikiBatchProperties properties) {
        KoikiBatchProperties.Io.File file = properties.getIo().getFile();
        return new DirectoryFileArchivePolicy(file.getArchiveDir(), file.getErrorDir());
    }

    @Bean
    @ConditionalOnBean(FileArchivePolicy.class)
    @ConditionalOnMissingBean
    public FileIngestionLifecycleListener fileIngestionLifecycleListener(FileArchivePolicy fileArchivePolicy) {
        return new FileIngestionLifecycleListener(fileArchivePolicy);
    }

    @Bean
    @ConditionalOnMissingBean
    public AtomicOutputListener atomicOutputListener(KoikiBatchProperties properties) {
        return new AtomicOutputListener(properties.getIo().getFile().getTempSuffix());
    }
}
