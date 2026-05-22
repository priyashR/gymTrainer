package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;

/**
 * Outbound port for pushing real-time session state updates to connected clients.
 * The implementation uses WebSocket/STOMP to push to /topic/sessions/{sessionId}.
 */
public interface SessionNotifier {

    /**
     * Pushes a session state update to subscribed clients.
     *
     * @param session the updated session state
     */
    void notifySessionUpdate(Session session);

    /**
     * Pushes a session completed notification to subscribed clients.
     *
     * @param session the completed session
     */
    void notifySessionCompleted(Session session);
}
