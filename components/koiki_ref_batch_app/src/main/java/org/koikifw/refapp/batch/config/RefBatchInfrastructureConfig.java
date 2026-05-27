package org.koikifw.refapp.batch.config;

import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Infrastructure for the reference app, which deliberately runs without a
 * database.
 *
 * <p>Spring Batch 6 uses a resourceless transaction manager internally for its
 * resourceless job repository, but it does not expose a
 * {@link PlatformTransactionManager} bean for step definitions to use. A
 * DB-less batch application must declare one explicitly. This is intentionally
 * an application concern: the framework does not provide a fallback transaction
 * manager, so a real application that forgets to wire its datasource-backed
 * manager fails fast instead of silently running non-transactional steps.</p>
 */
@Configuration
public class RefBatchInfrastructureConfig {

    @Bean
    @ConditionalOnMissingBean(PlatformTransactionManager.class)
    public PlatformTransactionManager resourcelessTransactionManager() {
        return new ResourcelessTransactionManager();
    }
}
