package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.CrossFitScore;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SetLog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Session performance-tracking domain logic:
 * hasPerformanceData() and duration computation.
 * Tests domain logic in isolation — no Spring context, no mocks.
 */
class SessionPerformanceTest {

    private static final String USER_ID = "user-123";
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final String WORKOUT_SNAPSHOT = "{\"sections\":[]}";
    private static final Instant BASE_TIME = Instant.parse("2026-01-15T10:00:00Z");

    // --- Test helpers ---

    private Session createSession(List<SectionProgress> sections) {
        return Session.start(
                UUID.randomUUID(), USER_ID, PROGRAM_ID, ENROLLMENT_ID,
                1, 1, sections, WORKOUT_SNAPSHOT, BASE_TIME
        );
    }

    private Session createSessionWithBuilder(List<SectionProgress> sections, long totalPausedSeconds, Instant startedAt) {
        return new Session.Builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .programId(PROGRAM_ID)
                .enrollmentId(ENROLLMENT_ID)
                .weekNumber(1)
                .dayNumber(1)
                .status(SessionStatus.IN_PROGRESS)
                .currentSectionIndex(0)
                .sectionProgresses(sections)
                .workoutSnapshot(WORKOUT_SNAPSHOT)
                .startedAt(startedAt)
                .totalPausedSeconds(totalPausedSeconds)
                .build();
    }

    // --- hasPerformanceData tests ---

    @Nested
    @DisplayName("hasPerformanceData")
    class HasPerformanceData {

        @Test
        @DisplayName("hasPerformanceData_NoSetsNoScores_ReturnsFalse")
        void hasPerformanceData_NoSetsNoScores_ReturnsFalse() {
            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Strength", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Squat"), new ExerciseLog(1, "Bench"))),
                    new SectionProgress(1, "AMRAP", SectionType.AMRAP,
                            List.of(new ExerciseLog(0, "Pull-ups")))
            );
            Session session = createSession(sections);

            assertFalse(session.hasPerformanceData());
        }

        @Test
        @DisplayName("hasPerformanceData_AtLeastOneSetExists_ReturnsTrue")
        void hasPerformanceData_AtLeastOneSetExists_ReturnsTrue() {
            ExerciseLog exerciseWithSet = new ExerciseLog(0, "Squat");
            exerciseWithSet.addSetLog(new SetLog(1, new BigDecimal("100"), 5, null, BASE_TIME));

            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Strength", SectionType.STRENGTH,
                            List.of(exerciseWithSet, new ExerciseLog(1, "Bench")))
            );
            Session session = createSession(sections);

            assertTrue(session.hasPerformanceData());
        }

        @Test
        @DisplayName("hasPerformanceData_AtLeastOneCrossFitScore_ReturnsTrue")
        void hasPerformanceData_AtLeastOneCrossFitScore_ReturnsTrue() {
            SectionProgress amrapSection = new SectionProgress(0, "AMRAP 20", SectionType.AMRAP,
                    List.of(new ExerciseLog(0, "Pull-ups")));
            amrapSection.setCrossFitScore(new CrossFitScore(5, 12, null, BASE_TIME));

            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Strength", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Squat"))),
                    amrapSection
            );
            // Need to use index 1 for the AMRAP section
            SectionProgress strengthSection = new SectionProgress(0, "Strength", SectionType.STRENGTH,
                    List.of(new ExerciseLog(0, "Squat")));
            SectionProgress scoredSection = new SectionProgress(1, "AMRAP 20", SectionType.AMRAP,
                    List.of(new ExerciseLog(0, "Pull-ups")));
            scoredSection.setCrossFitScore(new CrossFitScore(5, 12, null, BASE_TIME));

            Session session = createSession(List.of(strengthSection, scoredSection));

            assertTrue(session.hasPerformanceData());
        }

        @Test
        @DisplayName("hasPerformanceData_MultipleSectionsNoData_ReturnsFalse")
        void hasPerformanceData_MultipleSectionsNoData_ReturnsFalse() {
            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Warm Up", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Jumping Jacks"))),
                    new SectionProgress(1, "Main Lift", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Squat"), new ExerciseLog(1, "Bench"))),
                    new SectionProgress(2, "Conditioning", SectionType.AMRAP,
                            List.of(new ExerciseLog(0, "Burpees")))
            );
            Session session = createSession(sections);

            assertFalse(session.hasPerformanceData());
        }

        @Test
        @DisplayName("hasPerformanceData_SetInSecondSection_ReturnsTrue")
        void hasPerformanceData_SetInSecondSection_ReturnsTrue() {
            ExerciseLog exerciseWithSet = new ExerciseLog(0, "Bench Press");
            exerciseWithSet.addSetLog(new SetLog(1, new BigDecimal("80"), 8, new BigDecimal("7.0"), BASE_TIME));

            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Warm Up", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Jumping Jacks"))),
                    new SectionProgress(1, "Main Lift", SectionType.STRENGTH,
                            List.of(exerciseWithSet))
            );
            Session session = createSession(sections);

            assertTrue(session.hasPerformanceData());
        }
    }

    // --- Duration computation tests ---

    @Nested
    @DisplayName("computeDuration")
    class ComputeDuration {

        @Test
        @DisplayName("computeDuration_NoPauses_DurationEqualsElapsedTime")
        void computeDuration_NoPauses_DurationEqualsElapsedTime() {
            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Section 1", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Squat")))
            );
            Session session = createSessionWithBuilder(sections, 0, BASE_TIME);

            Instant endTime = BASE_TIME.plusSeconds(3600); // 1 hour later
            session.computeDuration(endTime);

            assertEquals(3600, session.getDurationSeconds());
        }

        @Test
        @DisplayName("computeDuration_OnePauseResumeCycle_ExcludesPausedTime")
        void computeDuration_OnePauseResumeCycle_ExcludesPausedTime() {
            // Session started at BASE_TIME, paused for 300 seconds total
            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Section 1", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Squat")))
            );
            Session session = createSessionWithBuilder(sections, 300, BASE_TIME);

            Instant endTime = BASE_TIME.plusSeconds(3600); // 1 hour total elapsed
            session.computeDuration(endTime);

            // Duration = 3600 - 300 = 3300 seconds
            assertEquals(3300, session.getDurationSeconds());
        }

        @Test
        @DisplayName("computeDuration_MultiplePauseResumeCycles_ExcludesAllPausedTime")
        void computeDuration_MultiplePauseResumeCycles_ExcludesAllPausedTime() {
            // Session started at BASE_TIME, total paused = 120 + 180 + 60 = 360 seconds
            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Section 1", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Squat")))
            );
            Session session = createSessionWithBuilder(sections, 360, BASE_TIME);

            Instant endTime = BASE_TIME.plusSeconds(1800); // 30 minutes total elapsed
            session.computeDuration(endTime);

            // Duration = 1800 - 360 = 1440 seconds
            assertEquals(1440, session.getDurationSeconds());
        }

        @Test
        @DisplayName("computeDuration_PausedTimeEqualsElapsed_DurationIsZero")
        void computeDuration_PausedTimeEqualsElapsed_DurationIsZero() {
            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Section 1", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Squat")))
            );
            // Edge case: paused for the entire duration
            Session session = createSessionWithBuilder(sections, 600, BASE_TIME);

            Instant endTime = BASE_TIME.plusSeconds(600);
            session.computeDuration(endTime);

            assertEquals(0, session.getDurationSeconds());
        }

        @Test
        @DisplayName("computeDuration_NullEndTime_ThrowsIllegalArgument")
        void computeDuration_NullEndTime_ThrowsIllegalArgument() {
            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Section 1", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Squat")))
            );
            Session session = createSessionWithBuilder(sections, 0, BASE_TIME);

            assertThrows(IllegalArgumentException.class, () ->
                    session.computeDuration(null)
            );
        }

        @Test
        @DisplayName("computeDuration_ShortSession_ComputesCorrectly")
        void computeDuration_ShortSession_ComputesCorrectly() {
            List<SectionProgress> sections = List.of(
                    new SectionProgress(0, "Section 1", SectionType.STRENGTH,
                            List.of(new ExerciseLog(0, "Squat")))
            );
            Session session = createSessionWithBuilder(sections, 0, BASE_TIME);

            Instant endTime = BASE_TIME.plusSeconds(45); // 45 seconds
            session.computeDuration(endTime);

            assertEquals(45, session.getDurationSeconds());
        }
    }
}
