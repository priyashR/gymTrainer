package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SetLog;

import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Property-based tests for SetLog domain object and ExerciseLog set logging.
 * Tests Properties 1, 2, and 3 from the design document.
 */
class SetLogPropertyTest {

    // --- Generators ---

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

    @Provide
    Arbitrary<BigDecimal> validRpeValues() {
        // RPE values: 1.0, 1.5, 2.0, ..., 9.5, 10.0 (19 possible values)
        return Arbitraries.integers().between(2, 20)
                .map(i -> new BigDecimal(i).divide(new BigDecimal("2")));
    }

    @Provide
    Arbitrary<BigDecimal> nullableValidRpe() {
        return Arbitraries.frequencyOf(
                Tuple.of(3, validRpeValues()),
                Tuple.of(1, Arbitraries.just((BigDecimal) null))
        );
    }

    @Provide
    Arbitrary<BigDecimal> invalidWeights() {
        return Arbitraries.oneOf(
                Arbitraries.just(BigDecimal.ZERO),
                Arbitraries.bigDecimals()
                        .between(new BigDecimal("-500.00"), new BigDecimal("-0.01"))
                        .ofScale(2)
        );
    }

    @Provide
    Arbitrary<Integer> invalidRepetitions() {
        return Arbitraries.integers().between(-100, 0);
    }

    @Provide
    Arbitrary<BigDecimal> invalidRpeValues() {
        return Arbitraries.oneOf(
                // Below range
                Arbitraries.bigDecimals()
                        .between(new BigDecimal("0.1"), new BigDecimal("0.9"))
                        .ofScale(1),
                // Above range
                Arbitraries.bigDecimals()
                        .between(new BigDecimal("10.1"), new BigDecimal("15.0"))
                        .ofScale(1),
                // Not 0.5 increment (e.g., 7.3, 4.1, 6.7)
                Arbitraries.of(
                        new BigDecimal("7.3"),
                        new BigDecimal("4.1"),
                        new BigDecimal("6.7"),
                        new BigDecimal("2.2"),
                        new BigDecimal("9.9"),
                        new BigDecimal("3.4"),
                        new BigDecimal("8.8")
                )
        );
    }

    // --- Property 1: Valid set log persistence and association ---

    /**
     * Property 1: Valid set log persistence and association.
     *
     * For any active session with a STRENGTH section, and any valid set log data
     * (weight > 0, repetitions > 0, RPE either null or in [1.0, 10.0] in 0.5 increments),
     * logging the set should append a SetLog to that ExerciseLog with the correct values
     * and a non-null timestamp.
     *
     * Validates: Requirements 1.1, 1.2, 1.3
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 1: Valid set log persistence and association")
    void validSetLog_isPersistedWithCorrectValues(
            @ForAll("validWeights") BigDecimal weight,
            @ForAll("validRepetitions") int repetitions,
            @ForAll("nullableValidRpe") BigDecimal rpe) {

        ExerciseLog exerciseLog = new ExerciseLog(0, "Bench Press");
        Instant before = Instant.now();

        int setNumber = exerciseLog.getSetLogs().size() + 1;
        Instant loggedAt = Instant.now();
        SetLog setLog = new SetLog(setNumber, weight, repetitions, rpe, loggedAt);
        exerciseLog.addSetLog(setLog);

        // Assert: SetLog appended to correct ExerciseLog
        List<SetLog> setLogs = exerciseLog.getSetLogs();
        assert setLogs.size() == 1 :
                "Expected 1 set log but got " + setLogs.size();

        SetLog persisted = setLogs.get(0);

        // Assert: correct values
        assert persisted.getSetNumber() == setNumber :
                "Expected setNumber " + setNumber + " but got " + persisted.getSetNumber();
        assert persisted.getWeight().compareTo(weight) == 0 :
                "Expected weight " + weight + " but got " + persisted.getWeight();
        assert persisted.getRepetitions() == repetitions :
                "Expected repetitions " + repetitions + " but got " + persisted.getRepetitions();
        if (rpe == null) {
            assert persisted.getRpe() == null :
                    "Expected null RPE but got " + persisted.getRpe();
        } else {
            assert persisted.getRpe() != null && persisted.getRpe().compareTo(rpe) == 0 :
                    "Expected RPE " + rpe + " but got " + persisted.getRpe();
        }

        // Assert: non-null timestamp at or after submission time
        assert persisted.getLoggedAt() != null :
                "loggedAt must not be null";
        assert !persisted.getLoggedAt().isBefore(before) :
                "loggedAt should be at or after submission time";
    }

    // --- Property 2: Multiple sets stored in chronological order ---

    /**
     * Property 2: Multiple sets stored in chronological order.
     *
     * For any sequence of N valid set logs submitted to the same exercise (N >= 1),
     * the resulting ExerciseLog should contain exactly N SetLog entries with setNumbers
     * 1 through N, and each entry's loggedAt timestamp should be less than or equal to
     * the next entry's loggedAt timestamp.
     *
     * Validates: Requirements 1.6
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 2: Multiple sets stored in chronological order")
    void multipleSets_storedInChronologicalOrder(
            @ForAll @IntRange(min = 1, max = 20) int numberOfSets,
            @ForAll("validWeights") BigDecimal weight,
            @ForAll("validRepetitions") int repetitions) {

        ExerciseLog exerciseLog = new ExerciseLog(0, "Squat");

        for (int i = 1; i <= numberOfSets; i++) {
            Instant loggedAt = Instant.now().plusMillis(i); // Ensure ordering
            SetLog setLog = new SetLog(i, weight, repetitions, null, loggedAt);
            exerciseLog.addSetLog(setLog);
        }

        List<SetLog> setLogs = exerciseLog.getSetLogs();

        // Assert: exactly N entries
        assert setLogs.size() == numberOfSets :
                "Expected " + numberOfSets + " set logs but got " + setLogs.size();

        // Assert: setNumbers are 1..N
        for (int i = 0; i < setLogs.size(); i++) {
            int expectedSetNumber = i + 1;
            assert setLogs.get(i).getSetNumber() == expectedSetNumber :
                    "Expected setNumber " + expectedSetNumber + " at index " + i
                            + " but got " + setLogs.get(i).getSetNumber();
        }

        // Assert: loggedAt non-decreasing
        for (int i = 1; i < setLogs.size(); i++) {
            Instant prev = setLogs.get(i - 1).getLoggedAt();
            Instant curr = setLogs.get(i).getLoggedAt();
            assert !curr.isBefore(prev) :
                    "loggedAt at index " + i + " (" + curr + ") is before index " + (i - 1) + " (" + prev + ")";
        }
    }

    // --- Property 3: Invalid set log rejection preserves state ---

    /**
     * Property 3: Invalid set log rejection preserves state.
     *
     * For any set log data where weight <= 0, or repetitions <= 0, or RPE is non-null
     * and outside [1.0, 10.0] or not a multiple of 0.5, the service should reject the
     * request and the session's state (including all existing SetLog entries) should
     * remain unchanged.
     *
     * Validates: Requirements 1.7
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 3: Invalid set log rejection preserves state — invalid weight")
    void invalidWeight_rejectsAndPreservesState(
            @ForAll("invalidWeights") BigDecimal invalidWeight,
            @ForAll("validRepetitions") int repetitions,
            @ForAll("nullableValidRpe") BigDecimal rpe) {

        ExerciseLog exerciseLog = new ExerciseLog(0, "Deadlift");

        // Add a valid set first to verify it's preserved
        SetLog validSet = new SetLog(1, new BigDecimal("100.0"), 5, null, Instant.now());
        exerciseLog.addSetLog(validSet);
        int sizeBefore = exerciseLog.getSetLogs().size();

        // Attempt to create invalid SetLog
        boolean exceptionThrown = false;
        try {
            new SetLog(2, invalidWeight, repetitions, rpe, Instant.now());
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        // Assert: exception thrown
        assert exceptionThrown :
                "Expected IllegalArgumentException for weight=" + invalidWeight;

        // Assert: state unchanged
        assert exerciseLog.getSetLogs().size() == sizeBefore :
                "SetLog list size changed after invalid attempt";
        assert exerciseLog.getSetLogs().get(0).getWeight().compareTo(new BigDecimal("100.0")) == 0 :
                "Existing set log was modified";
    }

    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 3: Invalid set log rejection preserves state — invalid repetitions")
    void invalidRepetitions_rejectsAndPreservesState(
            @ForAll("validWeights") BigDecimal weight,
            @ForAll("invalidRepetitions") int invalidReps,
            @ForAll("nullableValidRpe") BigDecimal rpe) {

        ExerciseLog exerciseLog = new ExerciseLog(0, "Deadlift");

        // Add a valid set first
        SetLog validSet = new SetLog(1, new BigDecimal("100.0"), 5, null, Instant.now());
        exerciseLog.addSetLog(validSet);
        int sizeBefore = exerciseLog.getSetLogs().size();

        // Attempt to create invalid SetLog
        boolean exceptionThrown = false;
        try {
            new SetLog(2, weight, invalidReps, rpe, Instant.now());
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        // Assert: exception thrown
        assert exceptionThrown :
                "Expected IllegalArgumentException for repetitions=" + invalidReps;

        // Assert: state unchanged
        assert exerciseLog.getSetLogs().size() == sizeBefore :
                "SetLog list size changed after invalid attempt";
    }

    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 3: Invalid set log rejection preserves state — invalid RPE")
    void invalidRpe_rejectsAndPreservesState(
            @ForAll("validWeights") BigDecimal weight,
            @ForAll("validRepetitions") int repetitions,
            @ForAll("invalidRpeValues") BigDecimal invalidRpe) {

        ExerciseLog exerciseLog = new ExerciseLog(0, "Deadlift");

        // Add a valid set first
        SetLog validSet = new SetLog(1, new BigDecimal("100.0"), 5, null, Instant.now());
        exerciseLog.addSetLog(validSet);
        int sizeBefore = exerciseLog.getSetLogs().size();

        // Attempt to create invalid SetLog
        boolean exceptionThrown = false;
        try {
            new SetLog(2, weight, repetitions, invalidRpe, Instant.now());
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        // Assert: exception thrown
        assert exceptionThrown :
                "Expected IllegalArgumentException for RPE=" + invalidRpe;

        // Assert: state unchanged
        assert exerciseLog.getSetLogs().size() == sizeBefore :
                "SetLog list size changed after invalid attempt";
    }
}
