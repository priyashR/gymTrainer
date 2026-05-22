package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for starting a new workout session.
 */
public record StartSessionRequest(
        @NotNull(message = "programId is required")
        UUID programId,

        @Min(value = 1, message = "weekNumber must be at least 1")
        int weekNumber,

        @Min(value = 1, message = "dayNumber must be at least 1")
        int dayNumber,

        boolean standalone
) {
}
