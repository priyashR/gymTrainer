package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.adapters.inbound.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for enrolling in a program.
 */
public record EnrollRequest(
        @NotNull(message = "programId is required")
        UUID programId,

        @NotBlank(message = "programName is required")
        String programName,

        @Min(value = 1, message = "totalWeeks must be at least 1")
        int totalWeeks,

        @Min(value = 1, message = "totalDaysPerWeek must be at least 1")
        int totalDaysPerWeek
) {
}
