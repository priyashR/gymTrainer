package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;

import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port for retrieving session state.
 * Enforces ownership — only the session owner can access their session.
 */
public interface GetSessionUseCase {

    /**
     * Retrieves a session by ID, verifying that it belongs to the given user.
     *
     * @param sessionId the session to retrieve
     * @param userId    the authenticated user's ID (for ownership check)
     * @return the session domain object
     * @throws com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.SessionNotFoundException if not found
     */
    Session getSession(UUID sessionId, String userId);

    /**
     * Retrieves the user's currently active (IN_PROGRESS or PAUSED) session, if any.
     *
     * @param userId the authenticated user's ID
     * @return the active session, or empty if none exists
     */
    Optional<Session> getActiveSession(String userId);
}
