package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto;

import jakarta.validation.constraints.Min;

/**
 * Request body for marking an exercise as completed.
 */
public record CompleteExerciseRequest(
        @Min(value = 0, message = "sectionIndex must be non-negative")
        int sectionIndex,

        @Min(value = 0, message = "exerciseIndex must be non-negative")
        int exerciseIndex
) {
}
