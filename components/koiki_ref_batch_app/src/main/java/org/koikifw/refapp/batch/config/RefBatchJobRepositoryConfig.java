package org.koikifw.refapp.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Enables a JDBC-backed {@code JobRepository} so batch metadata is persisted
 * (restart-safe) in the shared DataSource. Active only under the
 * {@code jdbc-repository} profile; without it the application runs on Spring
 * Batch 6's default {@code ResourcelessJobRepository} (see
 * {@code docs/batch/db-management-architecture.md}).
 *
 * <p>Spring Boot 4 / Spring Batch 6 default to a {@code ResourcelessJobRepository};
 * a DataSource alone does not switch to JDBC persistence. The JDBC store is opted
 * in explicitly here, reusing Boot's auto-configured {@code dataSource} and
 * {@code transactionManager} so business writes and step metadata commit in the
 * same transaction.</p>
 */
@Configuration
@Profile("jdbc-repository")
@EnableBatchProcessing
@EnableJdbcJobRepository(dataSourceRef = "dataSource", transactionManagerRef = "transactionManager")
public class RefBatchJobRepositoryConfig {
}
