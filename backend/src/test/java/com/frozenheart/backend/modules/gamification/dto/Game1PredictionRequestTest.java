package com.frozenheart.backend.modules.gamification.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Game1PredictionRequestTest {

    @Test
    void validatesParticipantCountAtBothBoundaries() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            assertThat(validator.validate(new Game1PredictionRequest(1, 10L))).isEmpty();
            assertThat(validator.validate(new Game1PredictionRequest(101, 10L))).isEmpty();
            assertThat(validator.validate(new Game1PredictionRequest(1_000_000, 10L))).isEmpty();

            assertThat(validator.validate(new Game1PredictionRequest(0, 10L)))
                    .singleElement().satisfies(violation ->
                            assertThat(violation.getPropertyPath().toString()).isEqualTo("predictedCount"));
            assertThat(validator.validate(new Game1PredictionRequest(1_000_001, 10L)))
                    .singleElement().satisfies(violation ->
                            assertThat(violation.getPropertyPath().toString()).isEqualTo("predictedCount"));
        }
    }
}
