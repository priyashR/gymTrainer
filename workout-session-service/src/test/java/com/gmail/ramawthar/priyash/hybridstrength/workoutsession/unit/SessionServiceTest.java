package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.event.SessionCompletedEvent;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.AccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.SessionAlreadyCompleteException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.SessionNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.AdvanceDayUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.GetEnrollmentUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.application.SessionService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.CrossFitScore;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SetLog;
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

import java.math.BigDecimal;
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

    // --- Performance Tracking Tests ---

    @Nested
    @DisplayName("logSet")
    class LogSet {

        private Session createSessionWithStrengthSection(String userId) {
            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Main Lift", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Back Squat"), new ExerciseLog(1, "Bench Press"))),
                    new SectionProgress(1, "Conditioning", SectionType.AMRAP,
                            List.of(new ExerciseLog(0, "Burpees")))
            );
            return Session.start(
                    UUID.randomUUID(), userId, PROGRAM_ID, ENROLLMENT_ID,
                    1, 1, sections, createValidProgramJson(), Instant.now()
            );
        }

        @Test
        @DisplayName("logSet_HappyPath_AppendsSetAndPersists")
        void logSet_HappyPath_AppendsSetAndPersists() {
            Session session = createSessionWithStrengthSection(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            Session result = sessionService.logSet(sessionId, USER_ID, 0, 0,
                    new BigDecimal("100.0"), 5, new BigDecimal("8.0"));

            List<SetLog> setLogs = result.getSectionProgresses().get(0).getExerciseLogs().get(0).getSetLogs();
            assertEquals(1, setLogs.size());
            assertEquals(1, setLogs.get(0).getSetNumber());
            assertEquals(0, new BigDecimal("100.0").compareTo(setLogs.get(0).getWeight()));
            assertEquals(5, setLogs.get(0).getRepetitions());
            assertEquals(0, new BigDecimal("8.0").compareTo(setLogs.get(0).getRpe()));

            verify(sessionRepository).save(any(Session.class));
            verify(sessionNotifier).notifySessionUpdate(any(Session.class));
        }

        @Test
        @DisplayName("logSet_MultipleSetsSameExercise_IncrementsSetNumber")
        void logSet_MultipleSetsSameExercise_IncrementsSetNumber() {
            Session session = createSessionWithStrengthSection(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            sessionService.logSet(sessionId, USER_ID, 0, 0, new BigDecimal("80"), 8, null);
            sessionService.logSet(sessionId, USER_ID, 0, 0, new BigDecimal("85"), 6, new BigDecimal("7.5"));
            Session result = sessionService.logSet(sessionId, USER_ID, 0, 0, new BigDecimal("90"), 5, new BigDecimal("9.0"));

            List<SetLog> setLogs = result.getSectionProgresses().get(0).getExerciseLogs().get(0).getSetLogs();
            assertEquals(3, setLogs.size());
            assertEquals(1, setLogs.get(0).getSetNumber());
            assertEquals(2, setLogs.get(1).getSetNumber());
            assertEquals(3, setLogs.get(2).getSetNumber());
        }

        @Test
        @DisplayName("logSet_CompletedSession_ThrowsSessionAlreadyComplete")
        void logSet_CompletedSession_ThrowsSessionAlreadyComplete() {
            Session session = createSessionWithStrengthSection(USER_ID);
            session.end(Instant.now());
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(SessionAlreadyCompleteException.class, () ->
                    sessionService.logSet(sessionId, USER_ID, 0, 0,
                            new BigDecimal("100"), 5, null)
            );
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("logSet_NonStrengthSection_ThrowsIllegalArgument")
        void logSet_NonStrengthSection_ThrowsIllegalArgument() {
            Session session = createSessionWithStrengthSection(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            // Section index 1 is AMRAP, not STRENGTH
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    sessionService.logSet(sessionId, USER_ID, 1, 0,
                            new BigDecimal("100"), 5, null)
            );
            assertTrue(ex.getMessage().contains("Set logging is only available for STRENGTH sections"));
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("logSet_WrongUser_ThrowsAccessDenied")
        void logSet_WrongUser_ThrowsAccessDenied() {
            Session session = createSessionWithStrengthSection(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(AccessDeniedException.class, () ->
                    sessionService.logSet(sessionId, OTHER_USER_ID, 0, 0,
                            new BigDecimal("100"), 5, null)
            );
        }

        @Test
        @DisplayName("logSet_InvalidSectionIndex_ThrowsIllegalArgument")
        void logSet_InvalidSectionIndex_ThrowsIllegalArgument() {
            Session session = createSessionWithStrengthSection(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(IllegalArgumentException.class, () ->
                    sessionService.logSet(sessionId, USER_ID, 99, 0,
                            new BigDecimal("100"), 5, null)
            );
        }

        @Test
        @DisplayName("logSet_InvalidExerciseIndex_ThrowsIllegalArgument")
        void logSet_InvalidExerciseIndex_ThrowsIllegalArgument() {
            Session session = createSessionWithStrengthSection(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(IllegalArgumentException.class, () ->
                    sessionService.logSet(sessionId, USER_ID, 0, 99,
                            new BigDecimal("100"), 5, null)
            );
        }

        @Test
        @DisplayName("logSet_NullRpe_AllowedAndPersists")
        void logSet_NullRpe_AllowedAndPersists() {
            Session session = createSessionWithStrengthSection(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            Session result = sessionService.logSet(sessionId, USER_ID, 0, 0,
                    new BigDecimal("60"), 10, null);

            SetLog logged = result.getSectionProgresses().get(0).getExerciseLogs().get(0).getSetLogs().get(0);
            assertNull(logged.getRpe());
        }
    }

    @Nested
    @DisplayName("logCrossFitScore")
    class LogCrossFitScore {

        private Session createSessionWithScoredSections(String userId) {
            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Strength", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Squat"))),
                    new SectionProgress(1, "AMRAP 20", SectionType.AMRAP,
                            List.of(new ExerciseLog(0, "Pull-ups"), new ExerciseLog(1, "Push-ups"))),
                    new SectionProgress(2, "For Time", SectionType.FOR_TIME,
                            List.of(new ExerciseLog(0, "Thrusters")))
            );
            return Session.start(
                    UUID.randomUUID(), userId, PROGRAM_ID, ENROLLMENT_ID,
                    1, 1, sections, createValidProgramJson(), Instant.now()
            );
        }

        @Test
        @DisplayName("logCrossFitScore_AmrapSection_SetsScoreAndPersists")
        void logCrossFitScore_AmrapSection_SetsScoreAndPersists() {
            Session session = createSessionWithScoredSections(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            Session result = sessionService.logCrossFitScore(sessionId, USER_ID, 1, 5, 12, null);

            CrossFitScore score = result.getSectionProgresses().get(1).getCrossFitScore();
            assertNotNull(score);
            assertEquals(5, score.getRounds());
            assertEquals(12, score.getAdditionalReps());
            assertNull(score.getTotalTimeSeconds());

            verify(sessionRepository).save(any(Session.class));
            verify(sessionNotifier).notifySessionUpdate(any(Session.class));
        }

        @Test
        @DisplayName("logCrossFitScore_ForTimeSection_SetsScoreWithTime")
        void logCrossFitScore_ForTimeSection_SetsScoreWithTime() {
            Session session = createSessionWithScoredSections(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            Session result = sessionService.logCrossFitScore(sessionId, USER_ID, 2, 3, 0, 720);

            CrossFitScore score = result.getSectionProgresses().get(2).getCrossFitScore();
            assertNotNull(score);
            assertEquals(3, score.getRounds());
            assertEquals(0, score.getAdditionalReps());
            assertEquals(720, score.getTotalTimeSeconds());
        }

        @Test
        @DisplayName("logCrossFitScore_NonScoredSection_ThrowsIllegalArgument")
        void logCrossFitScore_NonScoredSection_ThrowsIllegalArgument() {
            Session session = createSessionWithScoredSections(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            // Section 0 is STRENGTH, not a scored type
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    sessionService.logCrossFitScore(sessionId, USER_ID, 0, 5, 12, null)
            );
            assertTrue(ex.getMessage().contains("Score logging is only available for AMRAP, EMOM, or FOR_TIME sections"));
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("logCrossFitScore_CompletedSession_ThrowsSessionAlreadyComplete")
        void logCrossFitScore_CompletedSession_ThrowsSessionAlreadyComplete() {
            Session session = createSessionWithScoredSections(USER_ID);
            session.end(Instant.now());
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(SessionAlreadyCompleteException.class, () ->
                    sessionService.logCrossFitScore(sessionId, USER_ID, 1, 5, 12, null)
            );
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("logCrossFitScore_OverwriteSemantics_ReplacesExistingScore")
        void logCrossFitScore_OverwriteSemantics_ReplacesExistingScore() {
            Session session = createSessionWithScoredSections(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            // Log first score
            sessionService.logCrossFitScore(sessionId, USER_ID, 1, 3, 8, null);
            // Log second score — should overwrite
            Session result = sessionService.logCrossFitScore(sessionId, USER_ID, 1, 6, 15, null);

            CrossFitScore score = result.getSectionProgresses().get(1).getCrossFitScore();
            assertNotNull(score);
            assertEquals(6, score.getRounds());
            assertEquals(15, score.getAdditionalReps());
        }

        @Test
        @DisplayName("logCrossFitScore_ForTimeMissingTime_ThrowsIllegalArgument")
        void logCrossFitScore_ForTimeMissingTime_ThrowsIllegalArgument() {
            Session session = createSessionWithScoredSections(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            // FOR_TIME section (index 2) requires totalTimeSeconds
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    sessionService.logCrossFitScore(sessionId, USER_ID, 2, 3, 0, null)
            );
            assertTrue(ex.getMessage().contains("Total time must be greater than zero"));
        }

        @Test
        @DisplayName("logCrossFitScore_ForTimeZeroTime_ThrowsIllegalArgument")
        void logCrossFitScore_ForTimeZeroTime_ThrowsIllegalArgument() {
            Session session = createSessionWithScoredSections(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(IllegalArgumentException.class, () ->
                    sessionService.logCrossFitScore(sessionId, USER_ID, 2, 3, 0, 0)
            );
        }

        @Test
        @DisplayName("logCrossFitScore_WrongUser_ThrowsAccessDenied")
        void logCrossFitScore_WrongUser_ThrowsAccessDenied() {
            Session session = createSessionWithScoredSections(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(AccessDeniedException.class, () ->
                    sessionService.logCrossFitScore(sessionId, OTHER_USER_ID, 1, 5, 12, null)
            );
        }
    }

    @Nested
    @DisplayName("endSession — enriched event")
    class EndSessionEnrichedEvent {

        private Session createSessionWithPerformanceData(String userId) {
            ExerciseLog squat = new ExerciseLog(0, "Back Squat");
            squat.addSetLog(new SetLog(1, new BigDecimal("100"), 5, new BigDecimal("7.0"), Instant.now()));
            squat.addSetLog(new SetLog(2, new BigDecimal("110"), 3, new BigDecimal("8.5"), Instant.now()));

            ExerciseLog bench = new ExerciseLog(1, "Bench Press");
            // No sets logged for bench — should still appear in event

            SectionProgress strengthSection = new SectionProgress(0, "Main Lift", SectionType.STRENGTH,
                    List.of(squat, bench));

            SectionProgress amrapSection = new SectionProgress(1, "AMRAP 20", SectionType.AMRAP,
                    List.of(new ExerciseLog(0, "Pull-ups")));
            amrapSection.setCrossFitScore(new CrossFitScore(5, 12, null, Instant.now()));

            return Session.start(
                    UUID.randomUUID(), userId, PROGRAM_ID, ENROLLMENT_ID,
                    1, 1, List.of(strengthSection, amrapSection),
                    createValidProgramJson(), Instant.now()
            );
        }

        @Test
        @DisplayName("endSession_WithPerformanceData_EventIncludesDurationSeconds")
        void endSession_WithPerformanceData_EventIncludesDurationSeconds() {
            Session session = createSessionWithPerformanceData(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<SessionCompletedEvent> eventCaptor =
                    ArgumentCaptor.forClass(SessionCompletedEvent.class);

            sessionService.endSession(sessionId, USER_ID);

            verify(sessionEventPublisher).publishSessionCompleted(eventCaptor.capture());
            SessionCompletedEvent event = eventCaptor.getValue();

            assertNotNull(event.durationSeconds());
            assertTrue(event.durationSeconds() >= 0);
        }

        @Test
        @DisplayName("endSession_WithPerformanceData_EventContainsSetLogs")
        void endSession_WithPerformanceData_EventContainsSetLogs() {
            Session session = createSessionWithPerformanceData(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<SessionCompletedEvent> eventCaptor =
                    ArgumentCaptor.forClass(SessionCompletedEvent.class);

            sessionService.endSession(sessionId, USER_ID);

            verify(sessionEventPublisher).publishSessionCompleted(eventCaptor.capture());
            SessionCompletedEvent event = eventCaptor.getValue();

            // Verify set logs are present in the event
            List<SectionProgress> sections = event.sectionProgresses();
            assertEquals(2, sections.size());

            // Strength section has set logs
            List<SetLog> squatSets = sections.get(0).getExerciseLogs().get(0).getSetLogs();
            assertEquals(2, squatSets.size());
            assertEquals(1, squatSets.get(0).getSetNumber());
            assertEquals(2, squatSets.get(1).getSetNumber());
        }

        @Test
        @DisplayName("endSession_WithPerformanceData_EventContainsCrossFitScore")
        void endSession_WithPerformanceData_EventContainsCrossFitScore() {
            Session session = createSessionWithPerformanceData(USER_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<SessionCompletedEvent> eventCaptor =
                    ArgumentCaptor.forClass(SessionCompletedEvent.class);

            sessionService.endSession(sessionId, USER_ID);

            verify(sessionEventPublisher).publishSessionCompleted(eventCaptor.capture());
            SessionCompletedEvent event = eventCaptor.getValue();

            // Verify CrossFit score is present in the event
            CrossFitScore score = event.sectionProgresses().get(1).getCrossFitScore();
            assertNotNull(score);
            assertEquals(5, score.getRounds());
            assertEquals(12, score.getAdditionalReps());
        }

        @Test
        @DisplayName("endSession_NoPerformanceData_EventIncludesEmptyPerformanceFields")
        void endSession_NoPerformanceData_EventIncludesEmptyPerformanceFields() {
            Session session = createTestSession(USER_ID, ENROLLMENT_ID);
            UUID sessionId = session.getId();

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<SessionCompletedEvent> eventCaptor =
                    ArgumentCaptor.forClass(SessionCompletedEvent.class);

            sessionService.endSession(sessionId, USER_ID);

            verify(sessionEventPublisher).publishSessionCompleted(eventCaptor.capture());
            SessionCompletedEvent event = eventCaptor.getValue();

            // All sections and exercises should be present even without performance data
            assertEquals(2, event.sectionProgresses().size());

            // Verify empty set logs (not null)
            for (SectionProgress section : event.sectionProgresses()) {
                for (ExerciseLog exerciseLog : section.getExerciseLogs()) {
                    assertNotNull(exerciseLog.getSetLogs());
                    assertTrue(exerciseLog.getSetLogs().isEmpty());
                }
                // No CrossFit score for STRENGTH sections
                assertNull(section.getCrossFitScore());
            }

            // Duration should still be computed
            assertNotNull(event.durationSeconds());
        }
    }
}
