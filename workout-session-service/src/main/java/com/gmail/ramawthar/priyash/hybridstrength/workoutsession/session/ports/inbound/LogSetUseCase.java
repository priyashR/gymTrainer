package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inbound port for logging a strength set during an active workout session.
 * Appends a SetLog to the specified exercise within the specified section.
 */
public interface LogSetUseCase {

    /**
     * Logs a strength set for a specific exercise within a session.
     *
     * @param sessionId     the session to log the set in
     * @param userId        the authenticated user's ID (for ownership check)
     * @param sectionIndex  the section containing the exercise
     * @param exerciseIndex the exercise within the section
     * @param weight        the weight lifted in kilograms (must be positive)
     * @param repetitions   the number of repetitions performed (must be positive)
     * @param rpe           the Rate of Perceived Exertion (nullable, 1.0–10.0 in 0.5 increments)
     * @return the updated session state
     */
    Session logSet(UUID sessionId, String userId, int sectionIndex, int exerciseIndex,
                   BigDecimal weight, int repetitions, BigDecimal rpe);
}
