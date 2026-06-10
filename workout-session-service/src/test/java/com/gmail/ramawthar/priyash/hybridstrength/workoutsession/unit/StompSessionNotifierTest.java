package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.StompSessionNotifier;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.RecommendationsResponse.ExerciseRecommendationDto;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseRecommendation;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.RecommendationEngine;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SnapshotParseException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StompSessionNotifier recommendation enrichment.
 * Verifies that WebSocket messages include recommendations from the engine,
 * and that engine failures result in graceful degradation (empty list, message still sent).
 */
@ExtendWith(MockitoExtension.class)
class StompSessionNotifierTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final String USER_ID = "user-123";

    private static final String HYPERTROPHY_SNAPSHOT = """
            {
              "weeks": [
                {
                  "weekNumber": 1,
                  "days": [
                    {
                      "dayNumber": 1,
                      "modality": "HYPERTROPHY",
                      "sections": [
                        {
                          "name": "Main Lifts",
                          "type": "STRENGTH",
                          "exercises": [
                            {
                              "name": "Barbell Squat",
                              "sets": 4,
                              "reps": "8-10",
                              "weight": "80kg",
                              "restSeconds": 120
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RecommendationEngine recommendationEngine;

    private Session buildSession() {
        List<ExerciseLog> exerciseLogs = List.of(
                new ExerciseLog(0, "Barbell Squat", 120)
        );
        List<SectionProgress> sectionProgresses = List.of(
                new SectionProgress(0, "Main Lifts", SectionType.STRENGTH, exerciseLogs)
        );

        return new Session.Builder()
                .id(SESSION_ID)
                .userId(USER_ID)
                .programId(UUID.randomUUID())
                .enrollmentId(UUID.randomUUID())
                .weekNumber(1)
                .dayNumber(1)
                .status(SessionStatus.IN_PROGRESS)
                .currentSectionIndex(0)
                .sectionProgresses(sectionProgresses)
                .workoutSnapshot(HYPERTROPHY_SNAPSHOT)
                .startedAt(Instant.now())
                .lastPersistedAt(Instant.now())
                .totalPausedSeconds(0)
                .build();
    }

    @Test
    @DisplayName("notifySessionUpdate_ValidSession_MessageIncludesRecommendations")
    void notifySessionUpdate_ValidSession_MessageIncludesRecommendations() {
        // Arrange
        Session session = buildSession();
        List<ExerciseRecommendation> engineResult = List.of(
                new ExerciseRecommendation(0, 0, "80kg", "8-10", 4)
        );
        when(recommendationEngine.computeForSection(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(engineResult);

        StompSessionNotifier notifier = new StompSessionNotifier(messagingTemplate, recommendationEngine);

        // Act
        notifier.notifySessionUpdate(session);

        // Assert
        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/" + SESSION_ID),
                messageCaptor.capture()
        );

        Map<String, Object> message = messageCaptor.getValue();
        assertNotNull(message.get("recommendations"));

        @SuppressWarnings("unchecked")
        List<ExerciseRecommendationDto> recommendations =
                (List<ExerciseRecommendationDto>) message.get("recommendations");

        assertFalse(recommendations.isEmpty());
        assertEquals(1, recommendations.size());

        ExerciseRecommendationDto dto = recommendations.get(0);
        assertEquals(0, dto.exerciseIndex());
        assertEquals("80kg", dto.prescribedWeight());
        assertEquals("8-10", dto.prescribedReps());
        assertEquals(4, dto.prescribedSets());
    }

    @Test
    @DisplayName("notifySessionUpdate_EngineThrows_MessageIncludesEmptyRecommendations")
    void notifySessionUpdate_EngineThrows_MessageIncludesEmptyRecommendations() {
        // Arrange
        Session session = buildSession();
        when(recommendationEngine.computeForSection(anyString(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new SnapshotParseException("Malformed JSON"));

        StompSessionNotifier notifier = new StompSessionNotifier(messagingTemplate, recommendationEngine);

        // Act
        notifier.notifySessionUpdate(session);

        // Assert
        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/" + SESSION_ID),
                messageCaptor.capture()
        );

        Map<String, Object> message = messageCaptor.getValue();
        assertNotNull(message.get("recommendations"));

        @SuppressWarnings("unchecked")
        List<ExerciseRecommendationDto> recommendations =
                (List<ExerciseRecommendationDto>) message.get("recommendations");

        assertTrue(recommendations.isEmpty());
    }

    @Test
    @DisplayName("notifySessionUpdate_EngineThrows_MessageStillPublished")
    void notifySessionUpdate_EngineThrows_MessageStillPublished() {
        // Arrange
        Session session = buildSession();
        when(recommendationEngine.computeForSection(anyString(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Unexpected engine failure"));

        StompSessionNotifier notifier = new StompSessionNotifier(messagingTemplate, recommendationEngine);

        // Act
        notifier.notifySessionUpdate(session);

        // Assert — convertAndSend is still called (message is published despite engine failure)
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/" + SESSION_ID),
                any(Map.class)
        );
    }
}
