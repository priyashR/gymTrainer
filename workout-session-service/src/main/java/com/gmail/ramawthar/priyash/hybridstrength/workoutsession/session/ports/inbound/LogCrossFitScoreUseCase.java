package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;

import java.util.UUID;

/**
 * Inbound port for logging a CrossFit score during an active workout session.
 * Sets or overwrites the CrossFitScore on the specified section.
 */
public interface LogCrossFitScoreUseCase {

    /**
     * Logs a CrossFit score for a specific section within a session.
     *
     * @param sessionId        the session to log the score in
     * @param userId           the authenticated user's ID (for ownership check)
     * @param sectionIndex     the section to log the score for
     * @param rounds           the number of rounds completed (non-negative)
     * @param additionalReps   the additional reps in the final partial round (non-negative)
     * @param totalTimeSeconds the total time in seconds (nullable; required and positive for FOR_TIME)
     * @return the updated session state
     */
    Session logCrossFitScore(UUID sessionId, String userId, int sectionIndex,
                             int rounds, int additionalReps, Integer totalTimeSeconds);
}
