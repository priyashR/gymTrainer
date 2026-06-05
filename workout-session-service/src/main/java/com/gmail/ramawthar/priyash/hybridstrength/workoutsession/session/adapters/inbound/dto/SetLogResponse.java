package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response representation of a single logged strength set.
 */
public record SetLogResponse(
        int setNumber,
        BigDecimal weight,
        int repetitions,
        BigDecimal rpe,
        Instant loggedAt
) {
}
