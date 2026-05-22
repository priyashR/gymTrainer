package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.SessionResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionNotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * STOMP-based implementation of the SessionNotifier outbound port.
 * Pushes real-time session state updates to subscribed clients via WebSocket.
 *
 * Messages are sent to /topic/sessions/{sessionId} with a type discriminator
 * so the frontend can handle different message types.
 */
@Component
public class StompSessionNotifier implements SessionNotifier {

    private static final Logger log = LoggerFactory.getLogger(StompSessionNotifier.class);
    private static final String TOPIC_PREFIX = "/topic/sessions/";

    private final SimpMessagingTemplate messagingTemplate;

    public StompSessionNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notifySessionUpdate(Session session) {
        String destination = TOPIC_PREFIX + session.getId();

        Map<String, Object> message = Map.of(
                "type", "SESSION_STATE_UPDATE",
                "payload", SessionResponse.from(session)
        );

        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("Pushed SESSION_STATE_UPDATE to {}", destination);
        } catch (Exception e) {
            log.warn("Failed to push session update to WebSocket: session={}, error={}",
                    session.getId(), e.getMessage());
        }
    }

    @Override
    public void notifySessionCompleted(Session session) {
        String destination = TOPIC_PREFIX + session.getId();

        Map<String, Object> message = Map.of(
                "type", "SESSION_COMPLETED",
                "payload", Map.of(
                        "sessionId", session.getId().toString(),
                        "completedAt", session.getCompletedAt() != null
                                ? session.getCompletedAt().toString()
                                : Instant.now().toString()
                )
        );

        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("Pushed SESSION_COMPLETED to {}", destination);
        } catch (Exception e) {
            log.warn("Failed to push session completed to WebSocket: session={}, error={}",
                    session.getId(), e.getMessage());
        }
    }
}
