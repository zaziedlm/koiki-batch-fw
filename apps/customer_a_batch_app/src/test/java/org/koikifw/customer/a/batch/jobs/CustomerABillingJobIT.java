package org.koikifw.customer.a.batch.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class CustomerABillingJobIT {

    @Test
    void registersCustomerBillingConfiguration() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                CustomerABillingJobConfig.class)) {
            assertThat(context.getBean(CustomerABillingJobConfig.class)).isNotNull();
        }
    }
}
