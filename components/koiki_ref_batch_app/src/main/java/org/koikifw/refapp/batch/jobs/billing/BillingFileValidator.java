package org.koikifw.refapp.batch.jobs.billing;

import java.util.ArrayList;
import java.util.List;

import org.koikifw.libkoiki.batch.validation.ValidationError;
import org.koikifw.libkoiki.batch.validation.ValidationResult;
import org.koikifw.libkoiki.batch.validation.Validator;
import org.koikifw.refapp.batch.model.BillingFileRecord;

/**
 * Reference {@link Validator} for billing file records: {@code customerId} is
 * required and {@code amount} must not be negative.
 */
public class BillingFileValidator implements Validator<BillingFileRecord> {

    @Override
    public ValidationResult validate(BillingFileRecord record) {
        List<ValidationError> errors = new ArrayList<>();
        if (record.customerId() == null || record.customerId().isBlank()) {
            errors.add(new ValidationError("customerId", "must not be blank"));
        }
        if (record.amount() < 0) {
            errors.add(new ValidationError("amount", "must not be negative"));
        }
        return ValidationResult.of(errors);
    }
}
