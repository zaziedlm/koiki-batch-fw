package org.koikifw.refapp.batch.jobs.customer;

import org.koikifw.libkoiki.batch.validation.Validator;
import org.koikifw.refapp.batch.model.CustomerRecord;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * Chunk processor that applies the framework {@link Validator} contract to each
 * input record. Invalid records fail fast via
 * {@code ValidationResult.throwIfInvalid(...)}, which raises a
 * {@code ValidationException} (business error / exit code 20).
 */
public class CustomerValidatingProcessor implements ItemProcessor<CustomerRecord, CustomerRecord> {

    private final Validator<CustomerRecord> validator;

    public CustomerValidatingProcessor(Validator<CustomerRecord> validator) {
        this.validator = validator;
    }

    @Override
    public CustomerRecord process(CustomerRecord item) {
        validator.validate(item).throwIfInvalid("customer-import record id=" + item.id());
        return item;
    }
}
