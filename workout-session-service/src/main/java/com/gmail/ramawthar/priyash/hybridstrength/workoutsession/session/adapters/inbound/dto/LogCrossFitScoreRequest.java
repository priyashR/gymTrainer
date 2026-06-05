package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for logging a CrossFit score (AMRAP, EMOM, or FOR_TIME) during an active workout session.
 */
public record LogCrossFitScoreRequest(
        @NotNull(message = "sectionIndex must not be null")
        @Min(value = 0, message = "sectionIndex must be non-negative")
        Integer sectionIndex,

        @NotNull(message = "rounds must not be null")
        @Min(value = 0, message = "rounds must be non-negative")
        Integer rounds,

        @NotNull(message = "additionalReps must not be null")
        @Min(value = 0, message = "additionalReps must be non-negative")
        Integer additionalReps,

        @Min(value = 1, message = "totalTimeSeconds must be at least 1")
        Integer totalTimeSeconds
) {
}
