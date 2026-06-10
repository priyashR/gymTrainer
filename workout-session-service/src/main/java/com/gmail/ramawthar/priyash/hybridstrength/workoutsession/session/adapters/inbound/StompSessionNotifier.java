package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.RecommendationsResponse.ExerciseRecommendationDto;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.SessionResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseRecommendation;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.RecommendationEngine;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionNotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    private final RecommendationEngine recommendationEngine;

    @org.springframework.beans.factory.annotation.Autowired
    public StompSessionNotifier(SimpMessagingTemplate messagingTemplate) {
        this(messagingTemplate, new RecommendationEngine());
    }

    // Visible for testing — allows injecting a mock/stub RecommendationEngine
    public StompSessionNotifier(SimpMessagingTemplate messagingTemplate, RecommendationEngine recommendationEngine) {
        this.messagingTemplate = messagingTemplate;
        this.recommendationEngine = recommendationEngine;
    }

    @Override
    public void notifySessionUpdate(Session session) {
        String destination = TOPIC_PREFIX + session.getId();

        List<ExerciseRecommendationDto> recommendations = computeCurrentSectionRecommendations(session);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "SESSION_STATE_UPDATE");
        message.put("payload", SessionResponse.from(session));
        message.put("recommendations", recommendations);

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

        Map<String, Object> message = new HashMap<>();
        message.put("type", "SESSION_COMPLETED");
        message.put("payload", Map.of(
                "sessionId", session.getId().toString(),
                "completedAt", session.getCompletedAt() != null
                        ? session.getCompletedAt().toString()
                        : Instant.now().toString()
        ));

        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("Pushed SESSION_COMPLETED to {}", destination);
        } catch (Exception e) {
            log.warn("Failed to push session completed to WebSocket: session={}, error={}",
                    session.getId(), e.getMessage());
        }
    }

    /**
     * Computes recommendations for the session's current section.
     * If the engine fails for any reason, returns an empty list to ensure
     * the session state message is still published (graceful degradation).
     */
    private List<ExerciseRecommendationDto> computeCurrentSectionRecommendations(Session session) {
        try {
            List<ExerciseRecommendation> domainRecommendations = recommendationEngine.computeForSection(
                    session.getWorkoutSnapshot(),
                    session.getWeekNumber(),
                    session.getDayNumber(),
                    session.getCurrentSectionIndex()
            );
            return domainRecommendations.stream()
                    .map(rec -> new ExerciseRecommendationDto(
                            rec.exerciseIndex(),
                            rec.prescribedWeight(),
                            rec.prescribedReps(),
                            rec.prescribedSets()
                    ))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to compute recommendations for session={}, sectionIndex={}: {}",
                    session.getId(), session.getCurrentSectionIndex(), e.getMessage());
            return Collections.emptyList();
        }
    }
}
