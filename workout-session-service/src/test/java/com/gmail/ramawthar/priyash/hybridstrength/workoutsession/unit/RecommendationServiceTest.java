package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.AccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.SessionNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.application.RecommendationService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseRecommendation;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecommendationService application service.
 * All outbound ports are mocked — no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    private static final String USER_ID = "user-123";
    private static final String OTHER_USER_ID = "user-other";
    private static final UUID SESSION_ID = UUID.randomUUID();

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
                            },
                            {
                              "name": "Bench Press",
                              "sets": 3,
                              "reps": "10",
                              "weight": "60kg",
                              "restSeconds": 90
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
    private SessionRepository sessionRepository;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(sessionRepository);
    }

    private Session buildSession(String userId, String snapshot) {
        List<ExerciseLog> exerciseLogs = List.of(
                new ExerciseLog(0, "Barbell Squat", 120),
                new ExerciseLog(1, "Bench Press", 90)
        );
        List<SectionProgress> sectionProgresses = List.of(
                new SectionProgress(0, "Main Lifts", SectionType.STRENGTH, exerciseLogs)
        );

        return new Session.Builder()
                .id(SESSION_ID)
                .userId(userId)
                .programId(UUID.randomUUID())
                .enrollmentId(UUID.randomUUID())
                .weekNumber(1)
                .dayNumber(1)
                .status(SessionStatus.IN_PROGRESS)
                .currentSectionIndex(0)
                .sectionProgresses(sectionProgresses)
                .workoutSnapshot(snapshot)
                .startedAt(Instant.now())
                .lastPersistedAt(Instant.now())
                .totalPausedSeconds(0)
                .build();
    }

    @Nested
    @DisplayName("getRecommendations")
    class GetRecommendations {

        @Test
        @DisplayName("getRecommendations_ValidSessionAndOwner_ReturnsRecommendations")
        void getRecommendations_ValidSessionAndOwner_ReturnsRecommendations() {
            Session session = buildSession(USER_ID, HYPERTROPHY_SNAPSHOT);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            List<ExerciseRecommendation> result = recommendationService.getRecommendations(SESSION_ID, USER_ID);

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(2, result.size());

            ExerciseRecommendation first = result.get(0);
            assertEquals(0, first.sectionIndex());
            assertEquals(0, first.exerciseIndex());
            assertEquals("80kg", first.prescribedWeight());
            assertEquals("8-10", first.prescribedReps());
            assertEquals(4, first.prescribedSets());

            ExerciseRecommendation second = result.get(1);
            assertEquals(0, second.sectionIndex());
            assertEquals(1, second.exerciseIndex());
            assertEquals("60kg", second.prescribedWeight());
            assertEquals("10", second.prescribedReps());
            assertEquals(3, second.prescribedSets());

            verify(sessionRepository).findById(SESSION_ID);
        }

        @Test
        @DisplayName("getRecommendations_SessionNotFound_ThrowsSessionNotFoundException")
        void getRecommendations_SessionNotFound_ThrowsSessionNotFoundException() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(SessionNotFoundException.class,
                    () -> recommendationService.getRecommendations(SESSION_ID, USER_ID));

            verify(sessionRepository).findById(SESSION_ID);
        }

        @Test
        @DisplayName("getRecommendations_UserDoesNotOwnSession_ThrowsAccessDeniedException")
        void getRecommendations_UserDoesNotOwnSession_ThrowsAccessDeniedException() {
            Session session = buildSession(USER_ID, HYPERTROPHY_SNAPSHOT);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            assertThrows(AccessDeniedException.class,
                    () -> recommendationService.getRecommendations(SESSION_ID, OTHER_USER_ID));

            verify(sessionRepository).findById(SESSION_ID);
        }
    }

    @Nested
    @DisplayName("getRecommendationsForSection")
    class GetRecommendationsForSection {

        @Test
        @DisplayName("getRecommendationsForSection_ValidSessionAndOwner_ReturnsSectionRecommendations")
        void getRecommendationsForSection_ValidSessionAndOwner_ReturnsSectionRecommendations() {
            Session session = buildSession(USER_ID, HYPERTROPHY_SNAPSHOT);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            List<ExerciseRecommendation> result =
                    recommendationService.getRecommendationsForSection(SESSION_ID, USER_ID, 0);

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(r -> r.sectionIndex() == 0));

            verify(sessionRepository).findById(SESSION_ID);
        }

        @Test
        @DisplayName("getRecommendationsForSection_SessionNotFound_ThrowsSessionNotFoundException")
        void getRecommendationsForSection_SessionNotFound_ThrowsSessionNotFoundException() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(SessionNotFoundException.class,
                    () -> recommendationService.getRecommendationsForSection(SESSION_ID, USER_ID, 0));

            verify(sessionRepository).findById(SESSION_ID);
        }

        @Test
        @DisplayName("getRecommendationsForSection_UserDoesNotOwnSession_ThrowsAccessDeniedException")
        void getRecommendationsForSection_UserDoesNotOwnSession_ThrowsAccessDeniedException() {
            Session session = buildSession(USER_ID, HYPERTROPHY_SNAPSHOT);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            assertThrows(AccessDeniedException.class,
                    () -> recommendationService.getRecommendationsForSection(SESSION_ID, OTHER_USER_ID, 0));

            verify(sessionRepository).findById(SESSION_ID);
        }
    }
}
