package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Session domain object.
 * Tests domain logic in isolation — no Spring context, no mocks.
 */
class SessionTest {

    private static final String USER_ID = "user-123";
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final String WORKOUT_SNAPSHOT = "{\"sections\":[]}";

    // --- Test helpers ---

    private Session createInProgressSession(List<SectionProgress> sections) {
        return Session.start(
                UUID.randomUUID(),
                USER_ID,
                PROGRAM_ID,
                ENROLLMENT_ID,
                1, 1,
                sections,
                WORKOUT_SNAPSHOT,
                Instant.now()
        );
    }

    private SectionProgress createSection(int index, String name, SectionType type, int exerciseCount) {
        List<ExerciseLog> logs = new java.util.ArrayList<>();
        for (int i = 0; i < exerciseCount; i++) {
            logs.add(new ExerciseLog(i, "Exercise " + (i + 1)));
        }
        return new SectionProgress(index, name, type, logs);
    }

    private Session createSessionWithSections(int sectionCount, int exercisesPerSection) {
        List<SectionProgress> sections = new java.util.ArrayList<>();
        for (int i = 0; i < sectionCount; i++) {
            sections.add(createSection(i, "Section " + (i + 1), SectionType.STRENGTH, exercisesPerSection));
        }
        return createInProgressSession(sections);
    }

    // --- Tests ---

    @Nested
    @DisplayName("Session.start")
    class Start {

        @Test
        @DisplayName("start_ValidInputs_CreatesInProgressSession")
        void start_ValidInputs_CreatesInProgressSession() {
            List<SectionProgress> sections = List.of(
                    createSection(0, "Warm Up", SectionType.STRENGTH, 3)
            );

            Session session = Session.start(
                    UUID.randomUUID(), USER_ID, PROGRAM_ID, ENROLLMENT_ID,
                    2, 3, sections, WORKOUT_SNAPSHOT, Instant.now()
            );

            assertEquals(SessionStatus.IN_PROGRESS, session.getStatus());
            assertEquals(0, session.getCurrentSectionIndex());
            assertEquals(USER_ID, session.getUserId());
            assertEquals(2, session.getWeekNumber());
            assertEquals(3, session.getDayNumber());
            assertNotNull(session.getStartedAt());
            assertNull(session.getPausedAt());
            assertNull(session.getCompletedAt());
        }

        @Test
        @DisplayName("start_NullId_ThrowsIllegalArgument")
        void start_NullId_ThrowsIllegalArgument() {
            List<SectionProgress> sections = List.of(
                    createSection(0, "Section 1", SectionType.STRENGTH, 2)
            );

            assertThrows(IllegalArgumentException.class, () ->
                    Session.start(null, USER_ID, PROGRAM_ID, ENROLLMENT_ID,
                            1, 1, sections, WORKOUT_SNAPSHOT, Instant.now())
            );
        }

        @Test
        @DisplayName("start_EmptySections_ThrowsIllegalArgument")
        void start_EmptySections_ThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () ->
                    Session.start(UUID.randomUUID(), USER_ID, PROGRAM_ID, ENROLLMENT_ID,
                            1, 1, List.of(), WORKOUT_SNAPSHOT, Instant.now())
            );
        }

        @Test
        @DisplayName("start_NullProgramId_AllowedForStandalone")
        void start_NullProgramId_AllowedForStandalone() {
            List<SectionProgress> sections = List.of(
                    createSection(0, "Section 1", SectionType.AMRAP, 2)
            );

            Session session = Session.start(
                    UUID.randomUUID(), USER_ID, null, null,
                    1, 1, sections, WORKOUT_SNAPSHOT, Instant.now()
            );

            assertNull(session.getProgramId());
            assertNull(session.getEnrollmentId());
            assertEquals(SessionStatus.IN_PROGRESS, session.getStatus());
        }
    }

    @Nested
    @DisplayName("completeExercise")
    class CompleteExercise {

        @Test
        @DisplayName("completeExercise_ValidIndices_MarksExerciseCompleted")
        void completeExercise_ValidIndices_MarksExerciseCompleted() {
            Session session = createSessionWithSections(2, 3);
            Instant now = Instant.now();

            session.completeExercise(0, 1, now);

            ExerciseLog log = session.getSectionProgresses().get(0).getExerciseLogs().get(1);
            assertTrue(log.isCompleted());
            assertEquals(now, log.getCompletedAt());
        }

        @Test
        @DisplayName("completeExercise_AlreadyCompleted_IsIdempotent")
        void completeExercise_AlreadyCompleted_IsIdempotent() {
            Session session = createSessionWithSections(1, 2);
            Instant first = Instant.now();
            Instant second = first.plusSeconds(10);

            session.completeExercise(0, 0, first);
            session.completeExercise(0, 0, second);

            ExerciseLog log = session.getSectionProgresses().get(0).getExerciseLogs().get(0);
            assertTrue(log.isCompleted());
            // Timestamp should remain the first completion time (idempotent)
            assertEquals(first, log.getCompletedAt());
        }

        @Test
        @DisplayName("completeExercise_InvalidSectionIndex_ThrowsIllegalArgument")
        void completeExercise_InvalidSectionIndex_ThrowsIllegalArgument() {
            Session session = createSessionWithSections(2, 3);

            assertThrows(IllegalArgumentException.class, () ->
                    session.completeExercise(5, 0, Instant.now())
            );
        }

        @Test
        @DisplayName("completeExercise_NegativeSectionIndex_ThrowsIllegalArgument")
        void completeExercise_NegativeSectionIndex_ThrowsIllegalArgument() {
            Session session = createSessionWithSections(2, 3);

            assertThrows(IllegalArgumentException.class, () ->
                    session.completeExercise(-1, 0, Instant.now())
            );
        }

        @Test
        @DisplayName("completeExercise_InvalidExerciseIndex_ThrowsIllegalArgument")
        void completeExercise_InvalidExerciseIndex_ThrowsIllegalArgument() {
            Session session = createSessionWithSections(2, 3);

            assertThrows(IllegalArgumentException.class, () ->
                    session.completeExercise(0, 10, Instant.now())
            );
        }

        @Test
        @DisplayName("completeExercise_NegativeExerciseIndex_ThrowsIllegalArgument")
        void completeExercise_NegativeExerciseIndex_ThrowsIllegalArgument() {
            Session session = createSessionWithSections(2, 3);

            assertThrows(IllegalArgumentException.class, () ->
                    session.completeExercise(0, -1, Instant.now())
            );
        }

        @Test
        @DisplayName("completeExercise_CompletedSession_ThrowsIllegalState")
        void completeExercise_CompletedSession_ThrowsIllegalState() {
            Session session = createSessionWithSections(1, 2);
            session.end(Instant.now());

            assertThrows(IllegalStateException.class, () ->
                    session.completeExercise(0, 0, Instant.now())
            );
        }

        @Test
        @DisplayName("completeExercise_AllExercisesInSection_MarksSectionCompleted")
        void completeExercise_AllExercisesInSection_MarksSectionCompleted() {
            Session session = createSessionWithSections(1, 2);
            Instant now = Instant.now();

            session.completeExercise(0, 0, now);
            session.completeExercise(0, 1, now);

            assertTrue(session.getSectionProgresses().get(0).isCompleted());
        }
    }

    @Nested
    @DisplayName("advanceSection")
    class AdvanceSection {

        @Test
        @DisplayName("advanceSection_ValidTarget_UpdatesCurrentSectionIndex")
        void advanceSection_ValidTarget_UpdatesCurrentSectionIndex() {
            Session session = createSessionWithSections(4, 2);

            session.advanceSection(2);

            assertEquals(2, session.getCurrentSectionIndex());
        }

        @Test
        @DisplayName("advanceSection_BackwardNavigation_Allowed")
        void advanceSection_BackwardNavigation_Allowed() {
            Session session = createSessionWithSections(4, 2);
            session.advanceSection(3);

            session.advanceSection(1);

            assertEquals(1, session.getCurrentSectionIndex());
        }

        @Test
        @DisplayName("advanceSection_InvalidIndex_ThrowsIllegalArgument")
        void advanceSection_InvalidIndex_ThrowsIllegalArgument() {
            Session session = createSessionWithSections(3, 2);

            assertThrows(IllegalArgumentException.class, () ->
                    session.advanceSection(5)
            );
        }

        @Test
        @DisplayName("advanceSection_NegativeIndex_ThrowsIllegalArgument")
        void advanceSection_NegativeIndex_ThrowsIllegalArgument() {
            Session session = createSessionWithSections(3, 2);

            assertThrows(IllegalArgumentException.class, () ->
                    session.advanceSection(-1)
            );
        }

        @Test
        @DisplayName("advanceSection_CompletedSession_ThrowsIllegalState")
        void advanceSection_CompletedSession_ThrowsIllegalState() {
            Session session = createSessionWithSections(3, 2);
            session.end(Instant.now());

            assertThrows(IllegalStateException.class, () ->
                    session.advanceSection(1)
            );
        }
    }

    @Nested
    @DisplayName("pause")
    class Pause {

        @Test
        @DisplayName("pause_InProgressSession_TransitionsToPaused")
        void pause_InProgressSession_TransitionsToPaused() {
            Session session = createSessionWithSections(2, 3);
            Instant now = Instant.now();

            session.pause(now);

            assertEquals(SessionStatus.PAUSED, session.getStatus());
            assertEquals(now, session.getPausedAt());
        }

        @Test
        @DisplayName("pause_InProgressSession_PreservesProgress")
        void pause_InProgressSession_PreservesProgress() {
            Session session = createSessionWithSections(2, 3);
            Instant completionTime = Instant.now();
            session.completeExercise(0, 0, completionTime);
            session.completeExercise(0, 1, completionTime);
            session.advanceSection(1);

            session.pause(Instant.now());

            // Verify progress is preserved
            assertEquals(1, session.getCurrentSectionIndex());
            assertTrue(session.getSectionProgresses().get(0).getExerciseLogs().get(0).isCompleted());
            assertTrue(session.getSectionProgresses().get(0).getExerciseLogs().get(1).isCompleted());
            assertFalse(session.getSectionProgresses().get(0).getExerciseLogs().get(2).isCompleted());
            assertFalse(session.getSectionProgresses().get(1).getExerciseLogs().get(0).isCompleted());
        }

        @Test
        @DisplayName("pause_PausedSession_ThrowsIllegalState")
        void pause_PausedSession_ThrowsIllegalState() {
            Session session = createSessionWithSections(1, 2);
            session.pause(Instant.now());

            assertThrows(IllegalStateException.class, () ->
                    session.pause(Instant.now())
            );
        }

        @Test
        @DisplayName("pause_CompletedSession_ThrowsIllegalState")
        void pause_CompletedSession_ThrowsIllegalState() {
            Session session = createSessionWithSections(1, 2);
            session.end(Instant.now());

            assertThrows(IllegalStateException.class, () ->
                    session.pause(Instant.now())
            );
        }

        @Test
        @DisplayName("pause_NullTimestamp_ThrowsIllegalArgument")
        void pause_NullTimestamp_ThrowsIllegalArgument() {
            Session session = createSessionWithSections(1, 2);

            assertThrows(IllegalArgumentException.class, () ->
                    session.pause(null)
            );
        }
    }

    @Nested
    @DisplayName("end")
    class End {

        @Test
        @DisplayName("end_InProgressSession_TransitionsToCompleted")
        void end_InProgressSession_TransitionsToCompleted() {
            Session session = createSessionWithSections(2, 3);
            Instant now = Instant.now();

            session.end(now);

            assertEquals(SessionStatus.COMPLETED, session.getStatus());
            assertEquals(now, session.getCompletedAt());
        }

        @Test
        @DisplayName("end_PausedSession_TransitionsToCompleted")
        void end_PausedSession_TransitionsToCompleted() {
            Session session = createSessionWithSections(2, 3);
            session.pause(Instant.now());
            Instant endTime = Instant.now().plusSeconds(60);

            session.end(endTime);

            assertEquals(SessionStatus.COMPLETED, session.getStatus());
            assertEquals(endTime, session.getCompletedAt());
        }

        @Test
        @DisplayName("end_InProgressSession_PreservesLoggedProgress")
        void end_InProgressSession_PreservesLoggedProgress() {
            Session session = createSessionWithSections(2, 3);
            Instant completionTime = Instant.now();
            session.completeExercise(0, 0, completionTime);
            session.completeExercise(0, 2, completionTime);

            session.end(Instant.now());

            // Progress should be preserved after ending
            assertTrue(session.getSectionProgresses().get(0).getExerciseLogs().get(0).isCompleted());
            assertFalse(session.getSectionProgresses().get(0).getExerciseLogs().get(1).isCompleted());
            assertTrue(session.getSectionProgresses().get(0).getExerciseLogs().get(2).isCompleted());
            assertFalse(session.getSectionProgresses().get(1).getExerciseLogs().get(0).isCompleted());
        }

        @Test
        @DisplayName("end_CompletedSession_ThrowsIllegalState")
        void end_CompletedSession_ThrowsIllegalState() {
            Session session = createSessionWithSections(1, 2);
            session.end(Instant.now());

            assertThrows(IllegalStateException.class, () ->
                    session.end(Instant.now())
            );
        }

        @Test
        @DisplayName("end_NullTimestamp_ThrowsIllegalArgument")
        void end_NullTimestamp_ThrowsIllegalArgument() {
            Session session = createSessionWithSections(1, 2);

            assertThrows(IllegalArgumentException.class, () ->
                    session.end(null)
            );
        }
    }

    @Nested
    @DisplayName("computeNextUp")
    class ComputeNextUp {

        @Test
        @DisplayName("computeNextUp_UncompletedExerciseInCurrentSection_ReturnsExerciseName")
        void computeNextUp_UncompletedExerciseInCurrentSection_ReturnsExerciseName() {
            Session session = createSessionWithSections(2, 3);
            session.completeExercise(0, 0, Instant.now());

            Optional<String> nextUp = session.computeNextUp();

            assertTrue(nextUp.isPresent());
            assertEquals("Exercise 2", nextUp.get());
        }

        @Test
        @DisplayName("computeNextUp_CurrentSectionComplete_ReturnsNextSectionName")
        void computeNextUp_CurrentSectionComplete_ReturnsNextSectionName() {
            Session session = createSessionWithSections(3, 2);
            Instant now = Instant.now();
            // Complete all exercises in section 0
            session.completeExercise(0, 0, now);
            session.completeExercise(0, 1, now);

            Optional<String> nextUp = session.computeNextUp();

            assertTrue(nextUp.isPresent());
            assertEquals("Section 2", nextUp.get());
        }

        @Test
        @DisplayName("computeNextUp_AllSectionsComplete_ReturnsEmpty")
        void computeNextUp_AllSectionsComplete_ReturnsEmpty() {
            Session session = createSessionWithSections(2, 2);
            Instant now = Instant.now();
            session.completeExercise(0, 0, now);
            session.completeExercise(0, 1, now);
            session.completeExercise(1, 0, now);
            session.completeExercise(1, 1, now);

            Optional<String> nextUp = session.computeNextUp();

            assertTrue(nextUp.isEmpty());
        }

        @Test
        @DisplayName("computeNextUp_NoExercisesCompleted_ReturnsFirstExercise")
        void computeNextUp_NoExercisesCompleted_ReturnsFirstExercise() {
            Session session = createSessionWithSections(2, 3);

            Optional<String> nextUp = session.computeNextUp();

            assertTrue(nextUp.isPresent());
            assertEquals("Exercise 1", nextUp.get());
        }

        @Test
        @DisplayName("computeNextUp_CurrentSectionCompleteButOnLaterSection_ReturnsNextSection")
        void computeNextUp_CurrentSectionCompleteButOnLaterSection_ReturnsNextSection() {
            Session session = createSessionWithSections(3, 1);
            Instant now = Instant.now();
            // Complete section 0 and section 1
            session.completeExercise(0, 0, now);
            session.completeExercise(1, 0, now);
            // Navigate to section 1 (which is complete)
            session.advanceSection(1);

            Optional<String> nextUp = session.computeNextUp();

            assertTrue(nextUp.isPresent());
            assertEquals("Section 3", nextUp.get());
        }
    }

    @Nested
    @DisplayName("isAllComplete")
    class IsAllComplete {

        @Test
        @DisplayName("isAllComplete_NoExercisesCompleted_ReturnsFalse")
        void isAllComplete_NoExercisesCompleted_ReturnsFalse() {
            Session session = createSessionWithSections(2, 3);

            assertFalse(session.isAllComplete());
        }

        @Test
        @DisplayName("isAllComplete_SomeExercisesCompleted_ReturnsFalse")
        void isAllComplete_SomeExercisesCompleted_ReturnsFalse() {
            Session session = createSessionWithSections(2, 2);
            session.completeExercise(0, 0, Instant.now());
            session.completeExercise(0, 1, Instant.now());
            session.completeExercise(1, 0, Instant.now());

            assertFalse(session.isAllComplete());
        }

        @Test
        @DisplayName("isAllComplete_AllExercisesCompleted_ReturnsTrue")
        void isAllComplete_AllExercisesCompleted_ReturnsTrue() {
            Session session = createSessionWithSections(2, 2);
            Instant now = Instant.now();
            session.completeExercise(0, 0, now);
            session.completeExercise(0, 1, now);
            session.completeExercise(1, 0, now);
            session.completeExercise(1, 1, now);

            assertTrue(session.isAllComplete());
        }

        @Test
        @DisplayName("isAllComplete_SingleSectionAllComplete_ReturnsTrue")
        void isAllComplete_SingleSectionAllComplete_ReturnsTrue() {
            Session session = createSessionWithSections(1, 1);
            session.completeExercise(0, 0, Instant.now());

            assertTrue(session.isAllComplete());
        }
    }
}
