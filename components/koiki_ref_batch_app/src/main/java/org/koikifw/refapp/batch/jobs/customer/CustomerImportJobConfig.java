package org.koikifw.refapp.batch.jobs.customer;

import javax.sql.DataSource;

import org.koikifw.libkoiki.batch.core.KoikiBatchProperties;
import org.koikifw.libkoiki.batch.execution.KoikiJobParametersValidator;
import org.koikifw.libkoiki.batch.observability.JobLogListener;
import org.koikifw.libkoiki.batch.observability.StepLogListener;
import org.koikifw.libkoiki.batch.validation.Validator;
import org.koikifw.refapp.batch.model.CustomerRecord;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Reference DB-backed chunk job that demonstrates Phase 4: an explicit commit
 * boundary (the framework default commit interval) and the framework validation
 * contract applied inside the processor.
 *
 * <p>Reads from {@code customer_input}, validates each record, and writes valid
 * records to {@code customer}. Because batch metadata and business data share a
 * single {@code DataSource} / transaction manager, each chunk commit persists
 * business rows and step-execution metadata atomically.</p>
 */
@Configuration
public class CustomerImportJobConfig {

    public static final String JOB_NAME = "customer-import";

    @Bean
    public Validator<CustomerRecord> customerRecordValidator() {
        return new CustomerRecordValidator();
    }

    @Bean
    public JdbcCursorItemReader<CustomerRecord> customerInputReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<CustomerRecord>()
                .name("customerInputReader")
                .dataSource(dataSource)
                .sql("SELECT id, external_id, email FROM customer_input ORDER BY id")
                .rowMapper((rs, rowNum) ->
                        new CustomerRecord(rs.getLong("id"), rs.getString("external_id"), rs.getString("email")))
                .build();
    }

    @Bean
    public CustomerValidatingProcessor customerValidatingProcessor(Validator<CustomerRecord> customerRecordValidator) {
        return new CustomerValidatingProcessor(customerRecordValidator);
    }

    @Bean
    public JdbcBatchItemWriter<CustomerRecord> customerWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CustomerRecord>()
                .dataSource(dataSource)
                .sql("INSERT INTO customer (id, external_id, email) VALUES (:id, :externalId, :email)")
                .itemSqlParameterSourceProvider(item -> new MapSqlParameterSource()
                        .addValue("id", item.id())
                        .addValue("externalId", item.externalId())
                        .addValue("email", item.email()))
                .build();
    }

    @Bean
    public Step customerImportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            JdbcCursorItemReader<CustomerRecord> customerInputReader,
            CustomerValidatingProcessor customerValidatingProcessor,
            JdbcBatchItemWriter<CustomerRecord> customerWriter,
            KoikiBatchProperties properties, StepLogListener stepLogListener) {
        int commitInterval = properties.getTransaction().getDefaultCommitInterval();
        return new StepBuilder("customer-import-step", jobRepository)
                .<CustomerRecord, CustomerRecord>chunk(commitInterval)
                .reader(customerInputReader)
                .processor(customerValidatingProcessor)
                .writer(customerWriter)
                .transactionManager(transactionManager)
                .listener(stepLogListener)
                .build();
    }

    @Bean
    public Job customerImportJob(JobRepository jobRepository, Step customerImportStep,
            KoikiJobParametersValidator koikiJobParametersValidator, JobLogListener jobLogListener) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(koikiJobParametersValidator)
                .listener(jobLogListener)
                .start(customerImportStep)
                .build();
    }
}
