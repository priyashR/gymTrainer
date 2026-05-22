package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto;

import jakarta.validation.constraints.Min;

/**
 * Request body for navigating to a different section within a session.
 */
public record AdvanceSectionRequest(
        @Min(value = 0, message = "targetSectionIndex must be non-negative")
        int targetSectionIndex
) {
}
