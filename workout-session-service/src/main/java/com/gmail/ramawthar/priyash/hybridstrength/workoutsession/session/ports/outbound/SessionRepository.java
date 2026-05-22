package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting and retrieving workout sessions.
 */
public interface SessionRepository {

    /**
     * Persists a session (create or update).
     *
     * @param session the session to save
     * @return the saved session
     */
    Session save(Session session);

    /**
     * Finds a session by its unique ID.
     *
     * @param id the session ID
     * @return the session, or empty if not found
     */
    Optional<Session> findById(UUID id);

    /**
     * Finds the user's currently active session (IN_PROGRESS or PAUSED).
     *
     * @param userId the user's ID
     * @return the active session, or empty if none exists
     */
    Optional<Session> findActiveByUserId(String userId);
}
