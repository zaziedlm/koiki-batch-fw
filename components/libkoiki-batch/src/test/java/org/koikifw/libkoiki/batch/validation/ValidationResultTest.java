package org.koikifw.libkoiki.batch.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ValidationResultTest {

    @Test
    void validHasNoErrors() {
        ValidationResult result = ValidationResult.valid();

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void withErrorIsInvalid() {
        ValidationResult result = ValidationResult.withError("email", "must not be blank");

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors())
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.field()).isEqualTo("email");
                    assertThat(e.message()).isEqualTo("must not be blank");
                });
    }

    @Test
    void errorsAreImmutable() {
        List<ValidationError> source = new ArrayList<>();
        source.add(new ValidationError("a", "bad"));
        ValidationResult result = ValidationResult.of(source);

        // Mutating the source after construction must not affect the result.
        source.add(new ValidationError("b", "bad"));
        assertThat(result.errors()).hasSize(1);
        assertThatThrownBy(() -> result.errors().add(new ValidationError("c", "bad")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullErrorsBecomeEmpty() {
        assertThat(ValidationResult.of(null).isValid()).isTrue();
    }

    @Test
    void throwIfInvalidDoesNothingWhenValid() {
        assertThatCode(() -> ValidationResult.valid().throwIfInvalid("ctx")).doesNotThrowAnyException();
    }

    @Test
    void throwIfInvalidThrowsWhenInvalid() {
        ValidationResult result = ValidationResult.withError("email", "must not be blank");

        assertThatThrownBy(() -> result.throwIfInvalid("customer record"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("customer record")
                .hasMessageContaining("email")
                .hasMessageContaining("must not be blank");
    }
}
