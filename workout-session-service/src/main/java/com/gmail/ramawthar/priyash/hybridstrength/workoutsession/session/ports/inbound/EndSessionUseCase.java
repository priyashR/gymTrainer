package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;

import java.util.UUID;

/**
 * Inbound port for ending a workout session.
 * Marks the session as COMPLETED, publishes a SessionCompleted event,
 * and advances the program day pointer if the session is part of a program.
 */
public interface EndSessionUseCase {

    /**
     * Ends a session, marking it as completed with whatever progress has been logged.
     *
     * @param sessionId the session to end
     * @param userId    the authenticated user's ID (for ownership check)
     * @return the completed session state
     */
    Session endSession(UUID sessionId, String userId);
}
