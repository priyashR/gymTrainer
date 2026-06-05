package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto;

import java.time.Instant;

/**
 * Response representation of a CrossFit score for a section (AMRAP, EMOM, or FOR_TIME).
 */
public record CrossFitScoreResponse(
        int rounds,
        int additionalReps,
        Integer totalTimeSeconds,
        Instant loggedAt
) {
}
