package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;

import java.util.UUID;

/**
 * Inbound port for pausing and resuming a workout session.
 * Pausing persists the current state and stops all active timers.
 * Resuming transitions back to IN_PROGRESS from PAUSED.
 */
public interface PauseSessionUseCase {

    /**
     * Pauses an active session, preserving all progress.
     *
     * @param sessionId the session to pause
     * @param userId    the authenticated user's ID (for ownership check)
     * @return the updated session state
     */
    Session pauseSession(UUID sessionId, String userId);

    /**
     * Resumes a paused session, transitioning back to IN_PROGRESS.
     *
     * @param sessionId the session to resume
     * @param userId    the authenticated user's ID (for ownership check)
     * @return the updated session state
     */
    Session resumeSession(UUID sessionId, String userId);
}
