package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Property-based tests for Session domain logic.
 * Tests Properties 1–5 from the design document.
 */
class SessionPropertyTest {

    private static final String USER_ID = "user-prop-test";
    private static final String WORKOUT_SNAPSHOT = "{\"sections\":[]}";

    // --- Generators ---

    @Provide
    Arbitrary<SectionType> sectionTypes() {
        return Arbitraries.of(SectionType.values());
    }

    @Provide
    Arbitrary<List<SectionProgress>> workoutDefinitions() {
        Arbitrary<Integer> sectionCount = Arbitraries.integers().between(1, 5);
        return sectionCount.flatMap(numSections -> {
            List<Arbitrary<SectionProgress>> sectionArbitraries = new ArrayList<>();
            for (int i = 0; i < numSections; i++) {
                final int sectionIndex = i;
                Arbitrary<SectionProgress> sectionArb = Arbitraries.integers().between(1, 8)
                        .flatMap(exerciseCount -> sectionTypes().map(type -> {
                            List<ExerciseLog> logs = new ArrayList<>();
                            for (int e = 0; e < exerciseCount; e++) {
                                logs.add(new ExerciseLog(e, "Exercise " + (e + 1)));
                            }
                            return new SectionProgress(sectionIndex, "Section " + (sectionIndex + 1), type, logs);
                        }));
                sectionArbitraries.add(sectionArb);
            }
            return Combinators.combine(sectionArbitraries).as(sections -> sections);
        });
    }

    @Provide
    Arbitrary<Session> inProgressSessions() {
        return workoutDefinitions().map(sections ->
                Session.start(UUID.randomUUID(), USER_ID, UUID.randomUUID(), UUID.randomUUID(),
                        1, 1, sections, WORKOUT_SNAPSHOT, Instant.now())
        );
    }

    /**
     * Generates workout definitions where each exercise has a random restSeconds value
     * (either null or between 15 and 180 seconds).
     */
    @Provide
    Arbitrary<List<SectionProgress>> workoutDefinitionsWithRestSeconds() {
        Arbitrary<Integer> sectionCount = Arbitraries.integers().between(1, 5);
        return sectionCount.flatMap(numSections -> {
            List<Arbitrary<SectionProgress>> sectionArbitraries = new ArrayList<>();
            for (int i = 0; i < numSections; i++) {
                final int sectionIndex = i;
                Arbitrary<SectionProgress> sectionArb = Arbitraries.integers().between(1, 8)
                        .flatMap(exerciseCount -> sectionTypes().flatMap(type -> {
                            // Generate restSeconds for each exercise: null or 15–180
                            Arbitrary<Integer> restArb = Arbitraries.integers().between(15, 180);
                            Arbitrary<Integer> nullableRestArb = Arbitraries.frequencyOf(
                                    Tuple.of(3, restArb),
                                    Tuple.of(1, Arbitraries.just((Integer) null))
                            );
                            return nullableRestArb.list().ofSize(exerciseCount).map(restValues -> {
                                List<ExerciseLog> logs = new ArrayList<>();
                                for (int e = 0; e < exerciseCount; e++) {
                                    logs.add(new ExerciseLog(e, "Exercise " + (e + 1), restValues.get(e)));
                                }
                                return new SectionProgress(sectionIndex, "Section " + (sectionIndex + 1), type, logs);
                            });
                        }));
                sectionArbitraries.add(sectionArb);
            }
            return Combinators.combine(sectionArbitraries).as(sections -> sections);
        });
    }

    @Provide
    Arbitrary<Session> inProgressSessionsWithRestSeconds() {
        return workoutDefinitionsWithRestSeconds().map(sections ->
                Session.start(UUID.randomUUID(), USER_ID, UUID.randomUUID(), UUID.randomUUID(),
                        1, 1, sections, WORKOUT_SNAPSHOT, Instant.now())
        );
    }

    @Provide
    Arbitrary<Session> sessionsWithRandomProgress() {
        return workoutDefinitions().flatMap(sections -> {
            // Count total exercises to generate completion flags
            int totalExercises = sections.stream()
                    .mapToInt(sp -> sp.getExerciseLogs().size())
                    .sum();

            return Arbitraries.integers().between(0, (1 << Math.min(totalExercises, 20)) - 1)
                    .map(completionBits -> {
                        Session session = Session.start(UUID.randomUUID(), USER_ID, UUID.randomUUID(), UUID.randomUUID(),
                                1, 1, deepCopySections(sections), WORKOUT_SNAPSHOT, Instant.now());
                        int bitIdx = 0;
                        Instant now = Instant.now();
                        for (int s = 0; s < session.getSectionProgresses().size(); s++) {
                            SectionProgress sp = session.getSectionProgresses().get(s);
                            for (int e = 0; e < sp.getExerciseLogs().size(); e++) {
                                if (bitIdx < 20 && ((completionBits >> bitIdx) & 1) == 1) {
                                    session.completeExercise(s, e, now);
                                }
                                bitIdx++;
                            }
                        }
                        return session;
                    });
        });
    }

    private List<SectionProgress> deepCopySections(List<SectionProgress> original) {
        List<SectionProgress> copy = new ArrayList<>();
        for (SectionProgress sp : original) {
            List<ExerciseLog> logs = new ArrayList<>();
            for (ExerciseLog log : sp.getExerciseLogs()) {
                logs.add(new ExerciseLog(log.getExerciseIndex(), log.getExerciseName()));
            }
            copy.add(new SectionProgress(sp.getSectionIndex(), sp.getSectionName(), sp.getSectionType(), logs));
        }
        return copy;
    }

    // --- Property 1: Session initialization produces valid state ---

    /**
     * **Validates: Requirements 1.1**
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-active-workout, Property 1: Session initialization produces valid state")
    void sessionInitialization_producesValidState(@ForAll("workoutDefinitions") List<SectionProgress> sections) {
        Session session = Session.start(
                UUID.randomUUID(), USER_ID, UUID.randomUUID(), UUID.randomUUID(),
                1, 1, sections, WORKOUT_SNAPSHOT, Instant.now()
        );

        // Status must be IN_PROGRESS
        assert session.getStatus() == SessionStatus.IN_PROGRESS :
                "Expected IN_PROGRESS but got " + session.getStatus();

        // currentSectionIndex must be 0
        assert session.getCurrentSectionIndex() == 0 :
                "Expected currentSectionIndex=0 but got " + session.getCurrentSectionIndex();

        // sectionProgresses count must match input sections
        assert session.getSectionProgresses().size() == sections.size() :
                "Expected " + sections.size() + " sections but got " + session.getSectionProgresses().size();

        // All exercises must be not completed
        for (SectionProgress sp : session.getSectionProgresses()) {
            for (ExerciseLog log : sp.getExerciseLogs()) {
                assert !log.isCompleted() :
                        "Exercise " + log.getExerciseName() + " should not be completed on initialization";
                assert log.getCompletedAt() == null :
                        "Exercise " + log.getExerciseName() + " should have null completedAt on initialization";
            }
        }
    }

    // --- Property 2: Exercise completion records and returns rest duration ---

    /**
     * **Validates: Requirements 1.4**
     *
     * For any session in IN_PROGRESS status and any valid (sectionIndex, exerciseIndex) pair,
     * completing the exercise should mark it as completed with a non-null completedAt timestamp,
     * and the rest timer duration returned should equal the exercise definition's restSeconds
     * value (or the default 60s if null).
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-active-workout, Property 2: Exercise completion records and returns rest duration")
    void exerciseCompletion_marksCompletedAndReturnsRestDuration(
            @ForAll("inProgressSessionsWithRestSeconds") Session session,
            @ForAll @IntRange(min = 0, max = 4) int sectionHint,
            @ForAll @IntRange(min = 0, max = 7) int exerciseHint) {

        // Clamp indices to valid range for this session
        List<SectionProgress> sections = session.getSectionProgresses();
        int sectionIndex = sectionHint % sections.size();
        SectionProgress section = sections.get(sectionIndex);
        int exerciseIndex = exerciseHint % section.getExerciseLogs().size();

        // Capture the expected rest seconds before completion
        ExerciseLog exerciseBefore = section.getExerciseLogs().get(exerciseIndex);
        Integer configuredRestSeconds = exerciseBefore.getRestSeconds();
        int expectedRestSeconds = configuredRestSeconds != null
                ? configuredRestSeconds
                : ExerciseLog.DEFAULT_REST_SECONDS;

        Instant completionTime = Instant.now();
        int returnedRestSeconds = session.completeExercise(sectionIndex, exerciseIndex, completionTime);

        ExerciseLog log = session.getSectionProgresses().get(sectionIndex).getExerciseLogs().get(exerciseIndex);

        // Assert: exercise is marked completed
        assert log.isCompleted() :
                "Exercise at (" + sectionIndex + ", " + exerciseIndex + ") should be marked completed";

        // Assert: completedAt is non-null and equals the provided timestamp
        assert log.getCompletedAt() != null :
                "Exercise at (" + sectionIndex + ", " + exerciseIndex + ") should have non-null completedAt";
        assert log.getCompletedAt().equals(completionTime) :
                "completedAt should equal the provided timestamp";

        // Assert: returned rest duration equals the exercise's restSeconds (or default)
        assert returnedRestSeconds == expectedRestSeconds :
                "Expected rest duration " + expectedRestSeconds + "s but got " + returnedRestSeconds + "s"
                        + " (configured restSeconds=" + configuredRestSeconds + ")";
    }

    // --- Property 3: Next-up computation correctness ---

    /**
     * **Validates: Requirements 1.7, 4.9**
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-active-workout, Property 3: Next-up computation correctness")
    void nextUpComputation_returnsCorrectValue(@ForAll("sessionsWithRandomProgress") Session session) {
        Optional<String> nextUp = session.computeNextUp();

        SectionProgress currentSection = session.getSectionProgresses().get(session.getCurrentSectionIndex());

        // Find first uncompleted exercise in current section
        ExerciseLog firstUncompleted = null;
        for (ExerciseLog log : currentSection.getExerciseLogs()) {
            if (!log.isCompleted()) {
                firstUncompleted = log;
                break;
            }
        }

        if (firstUncompleted != null) {
            // Should return the first uncompleted exercise name
            assert nextUp.isPresent() :
                    "nextUp should be present when there are uncompleted exercises in current section";
            assert nextUp.get().equals(firstUncompleted.getExerciseName()) :
                    "Expected next-up '" + firstUncompleted.getExerciseName() + "' but got '" + nextUp.get() + "'";
        } else {
            // Current section is fully complete — look for next incomplete section
            String nextSectionName = null;
            for (int i = session.getCurrentSectionIndex() + 1; i < session.getSectionProgresses().size(); i++) {
                SectionProgress sp = session.getSectionProgresses().get(i);
                if (!sp.isAllExercisesCompleted()) {
                    nextSectionName = sp.getSectionName();
                    break;
                }
            }

            if (nextSectionName != null) {
                assert nextUp.isPresent() :
                        "nextUp should be present when there are incomplete sections after current";
                assert nextUp.get().equals(nextSectionName) :
                        "Expected next section '" + nextSectionName + "' but got '" + nextUp.get() + "'";
            } else {
                // All sections complete
                assert nextUp.isEmpty() :
                        "nextUp should be empty when all sections are complete";
            }
        }
    }

    // --- Property 4: Pause preserves session state ---

    /**
     * **Validates: Requirements 1.10**
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-active-workout, Property 4: Pause preserves session state")
    void pause_preservesSessionState(@ForAll("sessionsWithRandomProgress") Session session) {
        // Capture state before pause
        List<SectionProgress> progressesBefore = session.getSectionProgresses();
        List<List<Boolean>> completionStatesBefore = new ArrayList<>();
        List<List<Instant>> timestampsBefore = new ArrayList<>();
        for (SectionProgress sp : progressesBefore) {
            List<Boolean> sectionCompletions = new ArrayList<>();
            List<Instant> sectionTimestamps = new ArrayList<>();
            for (ExerciseLog log : sp.getExerciseLogs()) {
                sectionCompletions.add(log.isCompleted());
                sectionTimestamps.add(log.getCompletedAt());
            }
            completionStatesBefore.add(sectionCompletions);
            timestampsBefore.add(sectionTimestamps);
        }

        // Pause the session
        session.pause(Instant.now());

        // Verify status is PAUSED
        assert session.getStatus() == SessionStatus.PAUSED :
                "Expected PAUSED but got " + session.getStatus();

        // Verify all sectionProgresses data is unchanged
        List<SectionProgress> progressesAfter = session.getSectionProgresses();
        assert progressesAfter.size() == progressesBefore.size() :
                "Section count changed after pause";

        for (int s = 0; s < progressesAfter.size(); s++) {
            SectionProgress spAfter = progressesAfter.get(s);
            List<Boolean> expectedCompletions = completionStatesBefore.get(s);
            List<Instant> expectedTimestamps = timestampsBefore.get(s);

            for (int e = 0; e < spAfter.getExerciseLogs().size(); e++) {
                ExerciseLog logAfter = spAfter.getExerciseLogs().get(e);
                assert logAfter.isCompleted() == expectedCompletions.get(e) :
                        "Exercise completion state changed after pause at (" + s + ", " + e + ")";
                assert java.util.Objects.equals(logAfter.getCompletedAt(), expectedTimestamps.get(e)) :
                        "Exercise completedAt changed after pause at (" + s + ", " + e + ")";
            }
        }
    }

    // --- Property 5: End session preserves logged progress ---

    /**
     * **Validates: Requirements 1.11**
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-active-workout, Property 5: End session preserves logged progress")
    void endSession_preservesLoggedProgress(@ForAll("sessionsWithRandomProgress") Session session,
                                            @ForAll boolean pauseFirst) {
        // Optionally pause first (to test both IN_PROGRESS and PAUSED paths)
        if (pauseFirst) {
            session.pause(Instant.now());
        }

        // Capture state before end
        List<List<Boolean>> completionStatesBefore = new ArrayList<>();
        List<List<Instant>> timestampsBefore = new ArrayList<>();
        for (SectionProgress sp : session.getSectionProgresses()) {
            List<Boolean> sectionCompletions = new ArrayList<>();
            List<Instant> sectionTimestamps = new ArrayList<>();
            for (ExerciseLog log : sp.getExerciseLogs()) {
                sectionCompletions.add(log.isCompleted());
                sectionTimestamps.add(log.getCompletedAt());
            }
            completionStatesBefore.add(sectionCompletions);
            timestampsBefore.add(sectionTimestamps);
        }

        // End the session
        session.end(Instant.now());

        // Verify status is COMPLETED
        assert session.getStatus() == SessionStatus.COMPLETED :
                "Expected COMPLETED but got " + session.getStatus();

        // Verify completedAt is non-null
        assert session.getCompletedAt() != null :
                "completedAt should be non-null after ending session";

        // Verify all progress is preserved
        List<SectionProgress> progressesAfter = session.getSectionProgresses();
        for (int s = 0; s < progressesAfter.size(); s++) {
            SectionProgress spAfter = progressesAfter.get(s);
            List<Boolean> expectedCompletions = completionStatesBefore.get(s);
            List<Instant> expectedTimestamps = timestampsBefore.get(s);

            for (int e = 0; e < spAfter.getExerciseLogs().size(); e++) {
                ExerciseLog logAfter = spAfter.getExerciseLogs().get(e);
                assert logAfter.isCompleted() == expectedCompletions.get(e) :
                        "Exercise completion state changed after end at (" + s + ", " + e + ")";
                assert java.util.Objects.equals(logAfter.getCompletedAt(), expectedTimestamps.get(e)) :
                        "Exercise completedAt changed after end at (" + s + ", " + e + ")";
            }
        }
    }
}
