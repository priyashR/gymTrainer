package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound;

import java.util.UUID;

/**
 * Inbound port for starting a new workout session.
 * The service fetches the workout definition from the Vault, creates a session
 * with a snapshot of the workout, and returns the session ID.
 */
public interface StartSessionUseCase {

    /**
     * Starts a new workout session for the given user.
     *
     * @param userId     the authenticated user's ID
     * @param programId  the program containing the workout day
     * @param weekNumber the week within the program
     * @param dayNumber  the day within the week
     * @param standalone true if this session should not affect the user's program day pointer
     * @return the ID of the newly created session
     */
    UUID startSession(String userId, UUID programId, int weekNumber, int dayNumber, boolean standalone);
}
