package org.koikifw.libkoiki.batch.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.koikifw.libkoiki.batch.fault.BusinessException;
import org.koikifw.libkoiki.batch.fault.DefaultFaultClassifier;
import org.koikifw.libkoiki.batch.fault.FaultCategory;
import org.junit.jupiter.api.Test;

class ValidationExceptionTest {

    private final DefaultFaultClassifier classifier = new DefaultFaultClassifier();

    @Test
    void carriesErrorsAndSummarizesMessage() {
        ValidationException ex = new ValidationException("customer record",
                List.of(new ValidationError("email", "must not be blank"),
                        new ValidationError(null, "id already exists")));

        assertThat(ex.getErrors()).hasSize(2);
        assertThat(ex.getMessage())
                .contains("customer record")
                .contains("email: must not be blank")
                .contains("id already exists");
    }

    @Test
    void isABusinessExceptionMappedToExitCode20() {
        ValidationException ex = new ValidationException("ctx", List.of(new ValidationError("f", "bad")));

        assertThat(ex).isInstanceOf(BusinessException.class);
        assertThat(classifier.classify(ex)).isEqualTo(FaultCategory.BUSINESS_ERROR);
        assertThat(FaultCategory.BUSINESS_ERROR.exitCode()).isEqualTo(20);
    }

    @Test
    void classifiedAsBusinessErrorWhenWrappedAsCause() {
        ValidationException ex = new ValidationException("ctx", List.of(new ValidationError("f", "bad")));
        RuntimeException wrapper = new RuntimeException("step failed", ex);

        assertThat(classifier.classify(wrapper)).isEqualTo(FaultCategory.BUSINESS_ERROR);
    }

    @Test
    void handlesEmptyErrors() {
        ValidationException ex = new ValidationException(null, List.of());

        assertThat(ex.getErrors()).isEmpty();
        assertThat(ex.getMessage()).isEqualTo("Validation failed");
    }
}
