package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.event.SessionCompletedEvent;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.CrossFitScore;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SetLog;

import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Property-based tests for session-level performance tracking behavior.
 * Tests Properties 8 and 9 from the design document.
 */
class SessionPerformancePropertyTest {

    private static final String USER_ID = "user-perf-test";
    private static final String WORKOUT_SNAPSHOT = "{\"sections\":[]}";

    // --- Generators ---

    @Provide
    Arbitrary<SectionType> sectionTypes() {
        return Arbitraries.of(SectionType.values());
    }

    @Provide
    Arbitrary<BigDecimal> validWeights() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("500.00"))
                .ofScale(2);
    }

    @Provide
    Arbitrary<Integer> validRepetitions() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * Generates a random session with varying performance data:
     * - Some exercises have set logs, some don't
     * - Some sections have CrossFit scores, some don't
     */
    @Provide
    Arbitrary<Session> sessionsWithRandomPerformanceData() {
        return Arbitraries.integers().between(1, 5).flatMap(numSections -> {
            List<Arbitrary<SectionProgress>> sectionArbitraries = new ArrayList<>();
            for (int s = 0; s < numSections; s++) {
                final int sectionIndex = s;
                Arbitrary<SectionProgress> sectionArb = Combinators.combine(
                        Arbitraries.integers().between(1, 8),
                        sectionTypes(),
                        Arbitraries.of(true, false), // whether to add CrossFit score
                        Arbitraries.of(true, false)  // whether to add set logs to exercises
                ).as((exerciseCount, type, addScore, addSets) -> {
                    List<ExerciseLog> logs = new ArrayList<>();
                    for (int e = 0; e < exerciseCount; e++) {
                        ExerciseLog exerciseLog = new ExerciseLog(e, "Exercise " + (e + 1));
                        if (addSets && type == SectionType.STRENGTH) {
                            // Add 1-3 random set logs
                            int numSets = (e % 3) + 1;
                            for (int setNum = 1; setNum <= numSets; setNum++) {
                                SetLog setLog = new SetLog(
                                        setNum,
                                        new BigDecimal("60.0"),
                                        8,
                                        null,
                                        Instant.now().plusMillis(setNum)
                                );
                                exerciseLog.addSetLog(setLog);
                            }
                        }
                        logs.add(exerciseLog);
                    }

                    SectionProgress section = new SectionProgress(sectionIndex, "Section " + (sectionIndex + 1), type, logs);

                    if (addScore && (type == SectionType.AMRAP || type == SectionType.EMOM || type == SectionType.FOR_TIME)) {
                        Integer time = (type == SectionType.FOR_TIME) ? 300 : null;
                        CrossFitScore score = new CrossFitScore(5, 3, time, Instant.now());
                        section.setCrossFitScore(score);
                    }

                    return section;
                });
                sectionArbitraries.add(sectionArb);
            }
            return Combinators.combine(sectionArbitraries).as(sections -> {
                return Session.start(UUID.randomUUID(), USER_ID, UUID.randomUUID(), UUID.randomUUID(),
                        1, 1, sections, WORKOUT_SNAPSHOT, Instant.now());
            });
        });
    }

    /**
     * Generates sessions guaranteed to have NO performance data.
     */
    @Provide
    Arbitrary<Session> sessionsWithNoPerformanceData() {
        return Arbitraries.integers().between(1, 5).flatMap(numSections -> {
            List<Arbitrary<SectionProgress>> sectionArbitraries = new ArrayList<>();
            for (int s = 0; s < numSections; s++) {
                final int sectionIndex = s;
                Arbitrary<SectionProgress> sectionArb = Combinators.combine(
                        Arbitraries.integers().between(1, 8),
                        sectionTypes()
                ).as((exerciseCount, type) -> {
                    List<ExerciseLog> logs = new ArrayList<>();
                    for (int e = 0; e < exerciseCount; e++) {
                        logs.add(new ExerciseLog(e, "Exercise " + (e + 1)));
                    }
                    return new SectionProgress(sectionIndex, "Section " + (sectionIndex + 1), type, logs);
                });
                sectionArbitraries.add(sectionArb);
            }
            return Combinators.combine(sectionArbitraries).as(sections -> {
                return Session.start(UUID.randomUUID(), USER_ID, UUID.randomUUID(), UUID.randomUUID(),
                        1, 1, sections, WORKOUT_SNAPSHOT, Instant.now());
            });
        });
    }

    /**
     * Generates sessions guaranteed to have at least one set log.
     */
    @Provide
    Arbitrary<Session> sessionsWithSetLogs() {
        return Arbitraries.integers().between(1, 5).flatMap(numSections -> {
            List<Arbitrary<SectionProgress>> sectionArbitraries = new ArrayList<>();
            for (int s = 0; s < numSections; s++) {
                final int sectionIndex = s;
                Arbitrary<SectionProgress> sectionArb = Arbitraries.integers().between(1, 8)
                        .map(exerciseCount -> {
                            List<ExerciseLog> logs = new ArrayList<>();
                            for (int e = 0; e < exerciseCount; e++) {
                                ExerciseLog exerciseLog = new ExerciseLog(e, "Exercise " + (e + 1));
                                // Add a set log to the first exercise of the first section
                                if (sectionIndex == 0 && e == 0) {
                                    SetLog setLog = new SetLog(1, new BigDecimal("80.0"), 10, null, Instant.now());
                                    exerciseLog.addSetLog(setLog);
                                }
                                logs.add(exerciseLog);
                            }
                            return new SectionProgress(sectionIndex, "Section " + (sectionIndex + 1),
                                    SectionType.STRENGTH, logs);
                        });
                sectionArbitraries.add(sectionArb);
            }
            return Combinators.combine(sectionArbitraries).as(sections -> {
                return Session.start(UUID.randomUUID(), USER_ID, UUID.randomUUID(), UUID.randomUUID(),
                        1, 1, sections, WORKOUT_SNAPSHOT, Instant.now());
            });
        });
    }

    /**
     * Generates sessions guaranteed to have at least one CrossFit score.
     */
    @Provide
    Arbitrary<Session> sessionsWithCrossFitScores() {
        return Arbitraries.integers().between(1, 5).flatMap(numSections -> {
            List<Arbitrary<SectionProgress>> sectionArbitraries = new ArrayList<>();
            for (int s = 0; s < numSections; s++) {
                final int sectionIndex = s;
                Arbitrary<SectionProgress> sectionArb = Arbitraries.integers().between(1, 8)
                        .map(exerciseCount -> {
                            List<ExerciseLog> logs = new ArrayList<>();
                            for (int e = 0; e < exerciseCount; e++) {
                                logs.add(new ExerciseLog(e, "Exercise " + (e + 1)));
                            }
                            SectionProgress section = new SectionProgress(sectionIndex, "Section " + (sectionIndex + 1),
                                    SectionType.AMRAP, logs);
                            // Add a CrossFit score to the first section
                            if (sectionIndex == 0) {
                                CrossFitScore score = new CrossFitScore(4, 2, null, Instant.now());
                                section.setCrossFitScore(score);
                            }
                            return section;
                        });
                sectionArbitraries.add(sectionArb);
            }
            return Combinators.combine(sectionArbitraries).as(sections -> {
                return Session.start(UUID.randomUUID(), USER_ID, UUID.randomUUID(), UUID.randomUUID(),
                        1, 1, sections, WORKOUT_SNAPSHOT, Instant.now());
            });
        });
    }

    // --- Property 8: SessionCompleted event contains all performance data and timing ---

    /**
     * Property 8: SessionCompleted event contains all performance data and timing.
     *
     * For any completed session (with or without performance data), the SessionCompleted
     * event should contain: all existing fields, all SetLog entries for every ExerciseLog,
     * the CrossFitScore (or null) for every SectionProgress, and the durationSeconds field.
     * No exercises or sections should be omitted from the event regardless of whether they
     * have performance data.
     *
     * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 7.6
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 8: SessionCompleted event contains all performance data and timing")
    void sessionCompletedEvent_containsAllPerformanceDataAndTiming(
            @ForAll("sessionsWithRandomPerformanceData") Session session) {

        // End the session and compute duration
        Instant endTime = Instant.now().plusSeconds(3600);
        session.end(endTime);
        session.computeDuration(endTime);

        // Simulate building the SessionCompletedEvent (as SessionService does)
        SessionCompletedEvent event = new SessionCompletedEvent(
                UUID.randomUUID(),
                Instant.now(),
                session.getUserId(),
                session.getId(),
                session.getProgramId(),
                session.getWeekNumber(),
                session.getDayNumber(),
                session.getProgramId() == null,
                session.getSectionProgresses(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getDurationSeconds()
        );

        // Assert: all required fields present
        assert event.eventId() != null : "eventId must not be null";
        assert event.occurredAt() != null : "occurredAt must not be null";
        assert event.userId() != null : "userId must not be null";
        assert event.sessionId() != null : "sessionId must not be null";
        assert event.startedAt() != null : "startedAt must not be null";
        assert event.completedAt() != null : "completedAt must not be null";
        assert event.durationSeconds() != null : "durationSeconds must not be null";
        assert event.durationSeconds() >= 0 : "durationSeconds must be non-negative";

        // Assert: no sections omitted
        assert event.sectionProgresses() != null : "sectionProgresses must not be null";
        assert event.sectionProgresses().size() == session.getSectionProgresses().size() :
                "Event must contain all sections, expected " + session.getSectionProgresses().size()
                        + " but got " + event.sectionProgresses().size();

        // Assert: all exercises and performance data present in each section
        for (int s = 0; s < event.sectionProgresses().size(); s++) {
            SectionProgress eventSection = event.sectionProgresses().get(s);
            SectionProgress sessionSection = session.getSectionProgresses().get(s);

            // All exercises present
            assert eventSection.getExerciseLogs().size() == sessionSection.getExerciseLogs().size() :
                    "Section " + s + ": expected " + sessionSection.getExerciseLogs().size()
                            + " exercises but got " + eventSection.getExerciseLogs().size();

            // All SetLog entries present for each exercise
            for (int e = 0; e < eventSection.getExerciseLogs().size(); e++) {
                ExerciseLog eventExercise = eventSection.getExerciseLogs().get(e);
                ExerciseLog sessionExercise = sessionSection.getExerciseLogs().get(e);

                assert eventExercise.getSetLogs().size() == sessionExercise.getSetLogs().size() :
                        "Section " + s + ", Exercise " + e + ": expected "
                                + sessionExercise.getSetLogs().size() + " set logs but got "
                                + eventExercise.getSetLogs().size();
            }

            // CrossFitScore present (or null) matches session state
            CrossFitScore eventScore = eventSection.getCrossFitScore();
            CrossFitScore sessionScore = sessionSection.getCrossFitScore();
            if (sessionScore == null) {
                assert eventScore == null :
                        "Section " + s + ": expected null CrossFitScore but got non-null";
            } else {
                assert eventScore != null :
                        "Section " + s + ": expected non-null CrossFitScore but got null";
                assert eventScore.getRounds() == sessionScore.getRounds() :
                        "Section " + s + ": rounds mismatch";
                assert eventScore.getAdditionalReps() == sessionScore.getAdditionalReps() :
                        "Section " + s + ": additionalReps mismatch";
            }
        }
    }

    /**
     * Property 8 (complement): SessionCompleted event preserves empty performance fields.
     *
     * For sessions with NO performance data, the event should still include all sections
     * and exercises with empty set logs lists and null CrossFitScores — nothing omitted.
     *
     * Validates: Requirements 4.4, 7.6
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 8: SessionCompleted event includes empty performance fields when no data logged")
    void sessionCompletedEvent_includesEmptyPerformanceFieldsWhenNoDataLogged(
            @ForAll("sessionsWithNoPerformanceData") Session session) {

        // End the session and compute duration
        Instant endTime = Instant.now().plusSeconds(1800);
        session.end(endTime);
        session.computeDuration(endTime);

        // Build the event as SessionService does
        SessionCompletedEvent event = new SessionCompletedEvent(
                UUID.randomUUID(),
                Instant.now(),
                session.getUserId(),
                session.getId(),
                session.getProgramId(),
                session.getWeekNumber(),
                session.getDayNumber(),
                session.getProgramId() == null,
                session.getSectionProgresses(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getDurationSeconds()
        );

        // Assert: all required fields present
        assert event.eventId() != null : "eventId must not be null";
        assert event.occurredAt() != null : "occurredAt must not be null";
        assert event.userId() != null : "userId must not be null";
        assert event.sessionId() != null : "sessionId must not be null";
        assert event.startedAt() != null : "startedAt must not be null";
        assert event.completedAt() != null : "completedAt must not be null";
        assert event.durationSeconds() != null : "durationSeconds must not be null";
        assert event.durationSeconds() >= 0 : "durationSeconds must be non-negative";

        // Assert: no sections omitted even though there's no performance data
        assert event.sectionProgresses().size() == session.getSectionProgresses().size() :
                "Event must contain all sections even with no performance data";

        // Assert: all exercises present with empty performance fields
        for (int s = 0; s < event.sectionProgresses().size(); s++) {
            SectionProgress eventSection = event.sectionProgresses().get(s);
            SectionProgress sessionSection = session.getSectionProgresses().get(s);

            // All exercises present (not omitted)
            assert eventSection.getExerciseLogs().size() == sessionSection.getExerciseLogs().size() :
                    "Section " + s + ": all exercises must be present even with no performance data";

            // All exercises have empty set logs
            for (int e = 0; e < eventSection.getExerciseLogs().size(); e++) {
                ExerciseLog eventExercise = eventSection.getExerciseLogs().get(e);
                assert eventExercise.getSetLogs().isEmpty() :
                        "Section " + s + ", Exercise " + e + ": set logs should be empty";
            }

            // CrossFitScore should be null
            assert eventSection.getCrossFitScore() == null :
                    "Section " + s + ": CrossFitScore should be null when no score logged";
        }
    }

    // --- Property 9: Has-performance-data predicate correctness ---

    /**
     * Property 9: Has-performance-data predicate correctness.
     *
     * For any session state, the hasPerformanceData() predicate should return true if
     * and only if at least one ExerciseLog has a non-empty setLogs list OR at least one
     * SectionProgress has a non-null crossFitScore.
     *
     * Validates: Requirements 5.1, 5.4
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 9: Has-performance-data predicate — returns false when no data")
    void hasPerformanceData_returnsFalseWhenNoData(
            @ForAll("sessionsWithNoPerformanceData") Session session) {

        // Verify independently that there's no performance data
        boolean anySetLogs = session.getSectionProgresses().stream()
                .flatMap(sp -> sp.getExerciseLogs().stream())
                .anyMatch(el -> !el.getSetLogs().isEmpty());
        boolean anyScores = session.getSectionProgresses().stream()
                .anyMatch(sp -> sp.getCrossFitScore() != null);

        assert !anySetLogs && !anyScores :
                "Test setup error: session should have no performance data";

        // Assert: predicate returns false
        assert !session.hasPerformanceData() :
                "hasPerformanceData() should return false when no set logs and no CrossFit scores";
    }

    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 9: Has-performance-data predicate — returns true when set logs exist")
    void hasPerformanceData_returnsTrueWhenSetLogsExist(
            @ForAll("sessionsWithSetLogs") Session session) {

        // Assert: predicate returns true
        assert session.hasPerformanceData() :
                "hasPerformanceData() should return true when at least one set log exists";
    }

    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 9: Has-performance-data predicate — returns true when CrossFit scores exist")
    void hasPerformanceData_returnsTrueWhenCrossFitScoresExist(
            @ForAll("sessionsWithCrossFitScores") Session session) {

        // Assert: predicate returns true
        assert session.hasPerformanceData() :
                "hasPerformanceData() should return true when at least one CrossFit score exists";
    }

    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 9: Has-performance-data predicate — correctness for random state")
    void hasPerformanceData_correctnessForRandomState(
            @ForAll("sessionsWithRandomPerformanceData") Session session) {

        // Compute expected value independently
        boolean expectedHasData = false;
        for (SectionProgress sp : session.getSectionProgresses()) {
            if (sp.getCrossFitScore() != null) {
                expectedHasData = true;
                break;
            }
            for (ExerciseLog el : sp.getExerciseLogs()) {
                if (!el.getSetLogs().isEmpty()) {
                    expectedHasData = true;
                    break;
                }
            }
            if (expectedHasData) break;
        }

        // Assert: predicate matches independent computation
        assert session.hasPerformanceData() == expectedHasData :
                "hasPerformanceData() returned " + session.hasPerformanceData()
                        + " but expected " + expectedHasData;
    }
}
