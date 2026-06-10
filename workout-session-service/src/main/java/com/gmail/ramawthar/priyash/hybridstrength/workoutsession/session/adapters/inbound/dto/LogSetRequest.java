package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for logging a strength set during an active workout session.
 */
public record LogSetRequest(
        @NotNull(message = "sectionIndex must not be null")
        @Min(value = 0, message = "sectionIndex must be non-negative")
        Integer sectionIndex,

        @NotNull(message = "exerciseIndex must not be null")
        @Min(value = 0, message = "exerciseIndex must be non-negative")
        Integer exerciseIndex,

        @NotNull(message = "weight must not be null")
        @DecimalMin(value = "0.01", message = "weight must be at least 0.01")
        BigDecimal weight,

        @NotNull(message = "repetitions must not be null")
        @Min(value = 1, message = "repetitions must be at least 1")
        Integer repetitions,

        @DecimalMin(value = "1.0", message = "RPE must be at least 1.0")
        @DecimalMax(value = "10.0", message = "RPE must be at most 10.0")
        BigDecimal rpe
) {
}
