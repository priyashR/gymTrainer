package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Property-based tests for session duration computation.
 * Tests Property 10 from the design document.
 */
class DurationPropertyTest {

    private static final String USER_ID = "user-duration-test";
    private static final String WORKOUT_SNAPSHOT = "{\"sections\":[]}";

    // --- Generators ---

    /**
     * Represents a pause/resume pair with durations in seconds.
     */
    private record PausePeriod(long pauseAfterStartSeconds, long pauseDurationSeconds) {}

    @Provide
    Arbitrary<List<PausePeriod>> pauseSequences() {
        // Generate 0-5 pause/resume pairs with realistic durations
        Arbitrary<PausePeriod> pausePeriod = Combinators.combine(
                Arbitraries.longs().between(10, 600),   // pause happens 10-600s after start
                Arbitraries.longs().between(5, 300)     // pause lasts 5-300s
        ).as(PausePeriod::new);

        return pausePeriod.list().ofMinSize(0).ofMaxSize(5);
    }

    @Provide
    Arbitrary<Long> sessionDurations() {
        // Total session duration in seconds (5 minutes to 3 hours)
        return Arbitraries.longs().between(300, 10800);
    }

    private Session createSimpleSession(Instant startTime) {
        List<ExerciseLog> exercises = List.of(new ExerciseLog(0, "Bench Press"));
        List<SectionProgress> sections = List.of(
                new SectionProgress(0, "Strength", SectionType.STRENGTH, exercises)
        );
        return Session.start(UUID.randomUUID(), USER_ID, UUID.randomUUID(), UUID.randomUUID(),
                1, 1, sections, WORKOUT_SNAPSHOT, startTime);
    }

    // --- Property 10: Duration computation excludes paused time ---

    /**
     * Property 10: Duration computation excludes paused time.
     *
     * For any session timeline consisting of a start time, zero or more (pause, resume)
     * pairs, and an end time, the computed durationSeconds should equal
     * (endTime - startTime) - totalPausedSeconds, where totalPausedSeconds is the sum
     * of all (resumeTime - pauseTime) intervals. The duration should always be non-negative.
     *
     * Validates: Requirements 7.3
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 10: Duration computation excludes paused time")
    void durationComputation_excludesPausedTime(
            @ForAll("sessionDurations") long totalSessionSeconds,
            @ForAll("pauseSequences") List<PausePeriod> pausePeriods) {

        Instant startTime = Instant.parse("2026-01-01T10:00:00Z");

        // Build the session using the Builder to set totalPausedSeconds directly
        // (simulating what SessionService.resumeSession() would accumulate)
        long totalPausedSeconds = 0;
        for (PausePeriod period : pausePeriods) {
            totalPausedSeconds += period.pauseDurationSeconds();
        }

        // Ensure total paused time doesn't exceed session duration
        // (in reality, pauses can't exceed the total elapsed time)
        if (totalPausedSeconds >= totalSessionSeconds) {
            totalPausedSeconds = totalSessionSeconds / 2; // Cap at half the session
        }

        Instant endTime = startTime.plusSeconds(totalSessionSeconds);

        // Build session with accumulated paused seconds
        List<ExerciseLog> exercises = List.of(new ExerciseLog(0, "Bench Press"));
        List<SectionProgress> sections = List.of(
                new SectionProgress(0, "Strength", SectionType.STRENGTH, exercises)
        );

        Session session = new Session.Builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .programId(UUID.randomUUID())
                .enrollmentId(UUID.randomUUID())
                .weekNumber(1)
                .dayNumber(1)
                .status(SessionStatus.IN_PROGRESS)
                .currentSectionIndex(0)
                .sectionProgresses(sections)
                .workoutSnapshot(WORKOUT_SNAPSHOT)
                .startedAt(startTime)
                .totalPausedSeconds(totalPausedSeconds)
                .build();

        // End the session
        session.end(endTime);
        session.computeDuration(endTime);

        // Expected duration: (endTime - startTime) - totalPausedSeconds
        long expectedDuration = totalSessionSeconds - totalPausedSeconds;

        // Assert: durationSeconds is computed correctly
        assert session.getDurationSeconds() != null :
                "durationSeconds should not be null after computeDuration";
        assert session.getDurationSeconds() == (int) Math.max(0, expectedDuration) :
                "Expected durationSeconds=" + Math.max(0, expectedDuration)
                        + " but got " + session.getDurationSeconds()
                        + " (totalSession=" + totalSessionSeconds
                        + ", totalPaused=" + totalPausedSeconds + ")";

        // Assert: duration is always non-negative
        assert session.getDurationSeconds() >= 0 :
                "durationSeconds must always be non-negative, got " + session.getDurationSeconds();
    }

    /**
     * Property 10 (variant): Duration with no pauses equals total elapsed time.
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 10: Duration with no pauses equals total elapsed time")
    void durationWithNoPauses_equalsTotalElapsedTime(
            @ForAll("sessionDurations") long totalSessionSeconds) {

        Instant startTime = Instant.parse("2026-01-01T10:00:00Z");
        Instant endTime = startTime.plusSeconds(totalSessionSeconds);

        Session session = createSimpleSession(startTime);
        session.end(endTime);
        session.computeDuration(endTime);

        // Assert: duration equals total elapsed time when no pauses
        assert session.getDurationSeconds() != null :
                "durationSeconds should not be null";
        assert session.getDurationSeconds() == (int) totalSessionSeconds :
                "Expected durationSeconds=" + totalSessionSeconds
                        + " but got " + session.getDurationSeconds()
                        + " (no pauses, so duration should equal elapsed time)";
    }

    /**
     * Property 10 (variant): Duration is always non-negative even with large paused time.
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 10: Duration is always non-negative")
    void duration_isAlwaysNonNegative(
            @ForAll @IntRange(min = 60, max = 7200) int totalSessionSeconds,
            @ForAll @IntRange(min = 0, max = 10000) int totalPausedSeconds) {

        Instant startTime = Instant.parse("2026-01-01T10:00:00Z");
        Instant endTime = startTime.plusSeconds(totalSessionSeconds);

        List<ExerciseLog> exercises = List.of(new ExerciseLog(0, "Squat"));
        List<SectionProgress> sections = List.of(
                new SectionProgress(0, "Strength", SectionType.STRENGTH, exercises)
        );

        Session session = new Session.Builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .programId(UUID.randomUUID())
                .enrollmentId(UUID.randomUUID())
                .weekNumber(1)
                .dayNumber(1)
                .status(SessionStatus.IN_PROGRESS)
                .currentSectionIndex(0)
                .sectionProgresses(sections)
                .workoutSnapshot(WORKOUT_SNAPSHOT)
                .startedAt(startTime)
                .totalPausedSeconds(totalPausedSeconds)
                .build();

        session.end(endTime);
        session.computeDuration(endTime);

        // Assert: duration is always non-negative (Math.max(0, ...) in computeDuration)
        assert session.getDurationSeconds() != null :
                "durationSeconds should not be null";
        assert session.getDurationSeconds() >= 0 :
                "durationSeconds must always be non-negative, got " + session.getDurationSeconds()
                        + " (totalSession=" + totalSessionSeconds
                        + ", totalPaused=" + totalPausedSeconds + ")";
    }
}
