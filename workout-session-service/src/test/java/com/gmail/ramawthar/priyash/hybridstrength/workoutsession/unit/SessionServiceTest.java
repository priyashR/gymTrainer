package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.event.SessionCompletedEvent;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.AccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.SessionNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.AdvanceDayUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.GetEnrollmentUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.application.SessionService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionEventPublisher;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionNotifier;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionRepository;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.WorkoutFetcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SessionService application service.
 * All outbound ports are mocked — no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final String USER_ID = "user-789";
    private static final String OTHER_USER_ID = "user-other";
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private WorkoutFetcher workoutFetcher;

    @Mock
    private SessionEventPublisher sessionEventPublisher;

    @Mock
    private SessionNotifier sessionNotifier;

    @Mock
    private AdvanceDayUseCase advanceDayUseCase;

    @Mock
    private GetEnrollmentUseCase getEnrollmentUseCase;

    private SessionService sessionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(
                sessionRepository,
                workoutFetcher,
                sessionEventPublisher,
                sessionNotifier,
                advanceDayUseCase,
                getEnrollmentUseCase,
                objectMapper
        );
    }

    // --- Test helpers ---

    private String createValidProgramJson() {
        return """
                {
                  "weeks": [
                    {
                      "days": [
                        {
                          "sections": [
                            {
                              "name": "Warm Up",
                              "type": "STRENGTH",
                              "exercises": [
                                {"name": "Jumping Jacks"},
                                {"name": "Arm Circles"}
                              ]
                            },
                            {
                              "name": "Main Lift",
                              "type": "STRENGTH",
                              "exercises": [
                                {"name": "Back Squat"},
                                {"name": "Bench Press"}
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private Session createTestSession(String userId, UUID enrollmentId) {
        List<SectionProgress> sections = List.of(
                new SectionProgress(0, "Warm Up", SectionType.STRENGTH,
                        List.of(new ExerciseLog(0, "Jumping Jacks"), new ExerciseLog(1, "Arm Circles"))),
                new SectionProgress(1, "Main Lift", SectionType.STRENGTH,
                        List.of(new ExerciseLog(0, "Back Squat"), new ExerciseLog(1, "Bench Press")))
        );

        return Session.start(
                UUID.randomUUID(), userId, PROGRAM_ID, enrollmentId,
                1, 1, sections, createValidProgramJson(), Instant.now()
        );
    }

    // --- Tests ---

    @Nested
    @DisplayName("startSession")
    class StartSession {

        @Test
        @DisplayName("startSession_ValidRequest_CallsWorkoutFetcherAndPersists")
        void startSession_ValidRequest_CallsWorkoutFetcherAndPersists() {
            String programJson = createValidProgramJson();
            when(workoutFetcher.fetchProgram(PROGRAM_ID, null)).thenReturn(programJson);
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            UUID sessionId = sessionService.startSession(USER_ID, PROGRAM_ID, 1, 1, false);

            assertNotNull(sessionId);
            verify(workoutFetcher).fetchProgram(PROGRAM_ID, null);
            verify(sessionRepository).save(any(Session.class));
        }

        @Test
        @DisplayName("startSession_ValidRequest_CreatesSessionWithCorrectState")
        void startSession_ValidRequest_CreatesSessionWithCorrectState() {
            String programJson = createValidProgramJson();
            when(workoutFetcher.fetchProgram(PROGRAM_ID, null)).thenReturn(programJson);

            ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
            when(sessionRepository.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            sessionService.startSession(USER_ID, PROGRAM_ID, 1, 1, false);

            Session saved = sessionCaptor.getValue();
            assertEquals(SessionStatus.IN_PROGRESS, saved.getStatus());
            assertEquals(USER_ID, saved.getUserId());
            assertEquals(PROGRAM_ID, saved.getProgramId());
            assertEquals(0, saved.getCurrentSectionIndex());
            assertEquals(2, saved.getSectionProgresses().size());
            assertEquals("Warm Up", saved.getSectionProgresses().get(0).getSectionName());
            assertEquals("Main Lift", saved.getSectionProgresses().get(1).getSectionName());
        }

        @Test
        @DisplayName("startSession_InvalidProgramJson_ThrowsIllegalArgument")
        void startSession_InvalidProgramJson_ThrowsIllegalArgument() {
            when(workoutFetcher.fetchProgram(PROGRAM_ID, null)).thenReturn("invalid json");

            assertThrows(IllegalArgumentException.class, () ->
                    sessionService.startSession(USER_ID, PROGRAM_ID, 1, 1, false)
            );
        }
    }

    @Nested
    @DisplayName("completeExercise")
    class CompleteExercise {

        @Test
        @DisplayName("completeExercise_ValidRequest_DelegatesAndPersists")
        void completeExercise_ValidRequest_DelegatesAndPersists() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            Session result = sessionService.completeExercise(sessionId, USER_ID, 0, 0);

            assertTrue(result.getSectionProgresses().get(0).getExerciseLogs().get(0).isCompleted());
            verify(sessionRepository).save(any(Session.class));
            verify(sessionNotifier).notifySessionUpdate(any(Session.class));
        }

        @Test
        @DisplayName("completeExercise_SessionNotFound_ThrowsSessionNotFound")
        void completeExercise_SessionNotFound_ThrowsSessionNotFound() {
            UUID sessionId = UUID.randomUUID();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            assertThrows(SessionNotFoundException.class, () ->
                    sessionService.completeExercise(sessionId, USER_ID, 0, 0)
            );
        }

        @Test
        @DisplayName("completeExercise_WrongUser_ThrowsAccessDenied")
        void completeExercise_WrongUser_ThrowsAccessDenied() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(AccessDeniedException.class, () ->
                    sessionService.completeExercise(sessionId, OTHER_USER_ID, 0, 0)
            );
        }

        @Test
        @DisplayName("completeExercise_ValidRequest_PushesWebSocketUpdate")
        void completeExercise_ValidRequest_PushesWebSocketUpdate() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            sessionService.completeExercise(sessionId, USER_ID, 0, 1);

            verify(sessionNotifier).notifySessionUpdate(any(Session.class));
        }
    }

    @Nested
    @DisplayName("endSession")
    class EndSession {

        @Test
        @DisplayName("endSession_ProgramSession_MarksCompleteAndPublishesEventAndAdvancesDay")
        void endSession_ProgramSession_MarksCompleteAndPublishesEventAndAdvancesDay() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            Session result = sessionService.endSession(sessionId, USER_ID);

            assertEquals(SessionStatus.COMPLETED, result.getStatus());
            assertNotNull(result.getCompletedAt());

            // Verify event published
            verify(sessionEventPublisher).publishSessionCompleted(any(SessionCompletedEvent.class));

            // Verify day advanced for program session
            verify(advanceDayUseCase).advanceDay(ENROLLMENT_ID, USER_ID);

            // Verify WebSocket notification
            verify(sessionNotifier).notifySessionCompleted(any(Session.class));
        }

        @Test
        @DisplayName("endSession_StandaloneSession_DoesNotAdvanceDay")
        void endSession_StandaloneSession_DoesNotAdvanceDay() {
            // Standalone session has null enrollmentId
            Session session = createTestSession(USER_ID, null);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            Session result = sessionService.endSession(sessionId, USER_ID);

            assertEquals(SessionStatus.COMPLETED, result.getStatus());

            // Event still published
            verify(sessionEventPublisher).publishSessionCompleted(any(SessionCompletedEvent.class));

            // Day NOT advanced for standalone session
            verify(advanceDayUseCase, never()).advanceDay(any(), any());
        }

        @Test
        @DisplayName("endSession_PublishesCorrectEventPayload")
        void endSession_PublishesCorrectEventPayload() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<SessionCompletedEvent> eventCaptor =
                    ArgumentCaptor.forClass(SessionCompletedEvent.class);

            sessionService.endSession(sessionId, USER_ID);

            verify(sessionEventPublisher).publishSessionCompleted(eventCaptor.capture());
            SessionCompletedEvent event = eventCaptor.getValue();

            assertNotNull(event.eventId());
            assertNotNull(event.occurredAt());
            assertEquals(USER_ID, event.userId());
            assertEquals(sessionId, event.sessionId());
            assertEquals(PROGRAM_ID, event.programId());
            assertEquals(1, event.weekNumber());
            assertEquals(1, event.dayNumber());
            assertFalse(event.standalone());
            assertEquals(2, event.sectionProgresses().size());
        }

        @Test
        @DisplayName("endSession_WrongUser_ThrowsAccessDenied")
        void endSession_WrongUser_ThrowsAccessDenied() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(AccessDeniedException.class, () ->
                    sessionService.endSession(sessionId, OTHER_USER_ID)
            );
        }

        @Test
        @DisplayName("endSession_AdvanceDayFails_SessionStillCompleted")
        void endSession_AdvanceDayFails_SessionStillCompleted() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("DB error")).when(advanceDayUseCase)
                    .advanceDay(ENROLLMENT_ID, USER_ID);

            // Should not throw — session completion is not rolled back
            Session result = sessionService.endSession(sessionId, USER_ID);

            assertEquals(SessionStatus.COMPLETED, result.getStatus());
            verify(sessionEventPublisher).publishSessionCompleted(any(SessionCompletedEvent.class));
        }
    }

    @Nested
    @DisplayName("getSession")
    class GetSession {

        @Test
        @DisplayName("getSession_ValidOwner_ReturnsSession")
        void getSession_ValidOwner_ReturnsSession() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            Session result = sessionService.getSession(sessionId, USER_ID);

            assertNotNull(result);
            assertEquals(sessionId, result.getId());
        }

        @Test
        @DisplayName("getSession_WrongUser_ThrowsAccessDenied")
        void getSession_WrongUser_ThrowsAccessDenied() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(AccessDeniedException.class, () ->
                    sessionService.getSession(sessionId, OTHER_USER_ID)
            );
        }

        @Test
        @DisplayName("getSession_NotFound_ThrowsSessionNotFound")
        void getSession_NotFound_ThrowsSessionNotFound() {
            UUID sessionId = UUID.randomUUID();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            assertThrows(SessionNotFoundException.class, () ->
                    sessionService.getSession(sessionId, USER_ID)
            );
        }
    }

    @Nested
    @DisplayName("pauseSession")
    class PauseSession {

        @Test
        @DisplayName("pauseSession_InProgressSession_TransitionsToPaused")
        void pauseSession_InProgressSession_TransitionsToPaused() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            Session result = sessionService.pauseSession(sessionId, USER_ID);

            assertEquals(SessionStatus.PAUSED, result.getStatus());
            assertNotNull(result.getPausedAt());
            verify(sessionRepository).save(any(Session.class));
            verify(sessionNotifier).notifySessionUpdate(any(Session.class));
        }

        @Test
        @DisplayName("pauseSession_WrongUser_ThrowsAccessDenied")
        void pauseSession_WrongUser_ThrowsAccessDenied() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(AccessDeniedException.class, () ->
                    sessionService.pauseSession(sessionId, OTHER_USER_ID)
            );
        }
    }

    @Nested
    @DisplayName("resumeSession")
    class ResumeSession {

        @Test
        @DisplayName("resumeSession_PausedSession_TransitionsToInProgress")
        void resumeSession_PausedSession_TransitionsToInProgress() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            session.pause(Instant.now());
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            Session result = sessionService.resumeSession(sessionId, USER_ID);

            assertEquals(SessionStatus.IN_PROGRESS, result.getStatus());
            assertNull(result.getPausedAt());
            verify(sessionNotifier).notifySessionUpdate(any(Session.class));
        }

        @Test
        @DisplayName("resumeSession_InProgressSession_ThrowsIllegalState")
        void resumeSession_InProgressSession_ThrowsIllegalState() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(IllegalStateException.class, () ->
                    sessionService.resumeSession(sessionId, USER_ID)
            );
        }
    }
}
