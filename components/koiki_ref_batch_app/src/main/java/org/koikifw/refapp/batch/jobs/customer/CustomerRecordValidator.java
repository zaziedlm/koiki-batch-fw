package org.koikifw.refapp.batch.jobs.customer;

import java.util.ArrayList;
import java.util.List;

import org.koikifw.libkoiki.batch.validation.ValidationError;
import org.koikifw.libkoiki.batch.validation.ValidationResult;
import org.koikifw.libkoiki.batch.validation.Validator;
import org.koikifw.refapp.batch.model.CustomerRecord;

/**
 * Reference {@link Validator} implementation showing how an application plugs a
 * business rule into the framework validation contract: {@code externalId} is
 * required and {@code email} must look like an address.
 */
public class CustomerRecordValidator implements Validator<CustomerRecord> {

    @Override
    public ValidationResult validate(CustomerRecord record) {
        List<ValidationError> errors = new ArrayList<>();
        if (record.externalId() == null || record.externalId().isBlank()) {
            errors.add(new ValidationError("externalId", "must not be blank"));
        }
        if (record.email() == null || !record.email().contains("@")) {
            errors.add(new ValidationError("email", "must be a valid email address"));
        }
        return ValidationResult.of(errors);
    }
}
