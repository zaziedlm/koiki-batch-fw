package org.koikifw.refapp.batch.jobs.billing;

import java.nio.file.Path;

import org.koikifw.libkoiki.batch.core.KoikiBatchProperties;
import org.koikifw.libkoiki.batch.execution.KoikiJobParametersValidator;
import org.koikifw.libkoiki.batch.io.AtomicFileOutput;
import org.koikifw.libkoiki.batch.io.AtomicOutputListener;
import org.koikifw.libkoiki.batch.io.BatchCharsets;
import org.koikifw.libkoiki.batch.io.FileIngestionLifecycleListener;
import org.koikifw.libkoiki.batch.observability.JobLogListener;
import org.koikifw.libkoiki.batch.observability.StepLogListener;
import org.koikifw.libkoiki.batch.validation.Validator;
import org.koikifw.refapp.batch.model.BillingFileRecord;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Reference file&rarr;file chunk job demonstrating the Phase 5 IO value-adds:
 * configurable charset (A), input-file lifecycle (B), and atomic output (C).
 *
 * <p>Reads a delimited billing file ({@code customerId,amount}), validates each
 * record, and writes the result to an in-progress temp file. The framework's
 * {@code FileIngestionLifecycleListener} checks/archives the input and
 * {@code AtomicOutputListener} promotes the temp file to the final path only on
 * success, so the three concerns compose without interfering.</p>
 */
@Configuration
public class BillingFileJobConfig {

    public static final String JOB_NAME = "billing-file";

    @Bean
    public Validator<BillingFileRecord> billingFileValidator() {
        return new BillingFileValidator();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<BillingFileRecord> billingFileReader(
            @Value("#{jobParameters['" + FileIngestionLifecycleListener.INPUT_FILE_PARAMETER + "']}") String inputFile,
            KoikiBatchProperties properties) {
        String charset = BatchCharsets.resolve(properties.getIo().getFile().getCharset()).name();
        return new FlatFileItemReaderBuilder<BillingFileRecord>()
                .name("billingFileReader")
                .resource(new FileSystemResource(inputFile))
                .encoding(charset)
                .delimited().delimiter(",").names("customerId", "amount")
                .fieldSetMapper(fs -> new BillingFileRecord(fs.readString("customerId"), fs.readLong("amount")))
                .build();
    }

    @Bean
    public ItemProcessor<BillingFileRecord, BillingFileRecord> billingFileProcessor(
            Validator<BillingFileRecord> billingFileValidator) {
        return item -> {
            billingFileValidator.validate(item).throwIfInvalid("billing record customerId=" + item.customerId());
            return item;
        };
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<BillingFileRecord> billingFileWriter(
            @Value("#{jobParameters['" + AtomicOutputListener.OUTPUT_FILE_PARAMETER + "']}") String outputFile,
            KoikiBatchProperties properties) {
        String charset = BatchCharsets.resolve(properties.getIo().getFile().getCharset()).name();
        Path temp = AtomicFileOutput.tempPath(Path.of(outputFile), properties.getIo().getFile().getTempSuffix());
        return new FlatFileItemWriterBuilder<BillingFileRecord>()
                .name("billingFileWriter")
                .resource(new FileSystemResource(temp))
                .encoding(charset)
                .lineAggregator(item -> item.customerId() + "," + item.amount())
                .build();
    }

    @Bean
    public Step billingFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            FlatFileItemReader<BillingFileRecord> billingFileReader,
            ItemProcessor<BillingFileRecord, BillingFileRecord> billingFileProcessor,
            FlatFileItemWriter<BillingFileRecord> billingFileWriter,
            KoikiBatchProperties properties, StepLogListener stepLogListener) {
        int commitInterval = properties.getTransaction().getDefaultCommitInterval();
        return new StepBuilder("billing-file-step", jobRepository)
                .<BillingFileRecord, BillingFileRecord>chunk(commitInterval)
                .reader(billingFileReader)
                .processor(billingFileProcessor)
                .writer(billingFileWriter)
                .transactionManager(transactionManager)
                .listener(stepLogListener)
                .build();
    }

    @Bean
    public Job billingFileJob(JobRepository jobRepository, Step billingFileStep,
            KoikiJobParametersValidator koikiJobParametersValidator, JobLogListener jobLogListener,
            FileIngestionLifecycleListener fileIngestionLifecycleListener,
            AtomicOutputListener atomicOutputListener) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(koikiJobParametersValidator)
                .listener(jobLogListener)
                .listener(fileIngestionLifecycleListener)
                .listener(atomicOutputListener)
                .start(billingFileStep)
                .build();
    }
}
