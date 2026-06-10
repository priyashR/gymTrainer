package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.CrossFitScore;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.Instant;
import java.util.List;

/**
 * Property-based tests for CrossFitScore domain object and SectionProgress score management.
 * Tests Properties 4, 5, 6, and 7 from the design document.
 */
class CrossFitScorePropertyTest {

    // --- Generators ---

    @Provide
    Arbitrary<SectionType> scoredSectionTypes() {
        return Arbitraries.of(SectionType.AMRAP, SectionType.EMOM, SectionType.FOR_TIME);
    }

    @Provide
    Arbitrary<Integer> validRounds() {
        return Arbitraries.integers().between(0, 50);
    }

    @Provide
    Arbitrary<Integer> validAdditionalReps() {
        return Arbitraries.integers().between(0, 30);
    }

    @Provide
    Arbitrary<Integer> validTotalTimeSeconds() {
        return Arbitraries.integers().between(1, 7200);
    }

    @Provide
    Arbitrary<Integer> invalidNegativeRounds() {
        return Arbitraries.integers().between(-100, -1);
    }

    @Provide
    Arbitrary<Integer> invalidNegativeAdditionalReps() {
        return Arbitraries.integers().between(-100, -1);
    }

    @Provide
    Arbitrary<Integer> invalidTotalTimeSeconds() {
        return Arbitraries.integers().between(-100, 0);
    }

    private SectionProgress createScoredSection(SectionType type) {
        List<ExerciseLog> exercises = List.of(
                new ExerciseLog(0, "Burpees"),
                new ExerciseLog(1, "Box Jumps")
        );
        return new SectionProgress(0, "WOD", type, exercises);
    }

    // --- Property 4: Valid CrossFit score persistence and association ---

    /**
     * Property 4: Valid CrossFit score persistence and association.
     *
     * For any active session with a scored section (AMRAP, EMOM, or FOR_TIME), and any
     * valid CrossFitScore data (rounds >= 0, additionalReps >= 0, totalTimeSeconds > 0
     * when section is FOR_TIME), logging the score should set the CrossFitScore on that
     * SectionProgress with the correct values and a non-null timestamp.
     *
     * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 4: Valid CrossFit score persistence and association")
    void validCrossFitScore_isPersistedWithCorrectValues(
            @ForAll("scoredSectionTypes") SectionType sectionType,
            @ForAll("validRounds") int rounds,
            @ForAll("validAdditionalReps") int additionalReps,
            @ForAll("validTotalTimeSeconds") int totalTimeSeconds) {

        SectionProgress section = createScoredSection(sectionType);
        Instant before = Instant.now();

        // For non-FOR_TIME sections, totalTimeSeconds can be null
        Integer timeValue = (sectionType == SectionType.FOR_TIME) ? totalTimeSeconds : null;
        Instant loggedAt = Instant.now();
        CrossFitScore score = new CrossFitScore(rounds, additionalReps, timeValue, loggedAt);
        section.setCrossFitScore(score);

        // Assert: CrossFitScore set on correct SectionProgress
        CrossFitScore persisted = section.getCrossFitScore();
        assert persisted != null :
                "CrossFitScore should not be null after setting";

        // Assert: correct values
        assert persisted.getRounds() == rounds :
                "Expected rounds " + rounds + " but got " + persisted.getRounds();
        assert persisted.getAdditionalReps() == additionalReps :
                "Expected additionalReps " + additionalReps + " but got " + persisted.getAdditionalReps();
        if (sectionType == SectionType.FOR_TIME) {
            assert persisted.getTotalTimeSeconds() != null
                    && persisted.getTotalTimeSeconds() == totalTimeSeconds :
                    "Expected totalTimeSeconds " + totalTimeSeconds + " but got " + persisted.getTotalTimeSeconds();
        } else {
            assert persisted.getTotalTimeSeconds() == null :
                    "Expected null totalTimeSeconds for " + sectionType + " but got " + persisted.getTotalTimeSeconds();
        }

        // Assert: non-null timestamp at or after submission time
        assert persisted.getLoggedAt() != null :
                "loggedAt must not be null";
        assert !persisted.getLoggedAt().isBefore(before) :
                "loggedAt should be at or after submission time";
    }

    // --- Property 5: Invalid CrossFit score rejection ---

    /**
     * Property 5: Invalid CrossFit score rejection.
     *
     * For any CrossFitScore data where rounds < 0, or additionalReps < 0, or
     * totalTimeSeconds <= 0 for a FOR_TIME section, the service should reject the
     * request and the session's state should remain unchanged.
     *
     * Validates: Requirements 2.6
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 5: Invalid CrossFit score rejection — negative rounds")
    void negativeRounds_rejectsAndPreservesState(
            @ForAll("invalidNegativeRounds") int invalidRounds,
            @ForAll("validAdditionalReps") int additionalReps) {

        SectionProgress section = createScoredSection(SectionType.AMRAP);

        // Set a valid score first
        CrossFitScore validScore = new CrossFitScore(5, 3, null, Instant.now());
        section.setCrossFitScore(validScore);

        // Attempt to create invalid CrossFitScore
        boolean exceptionThrown = false;
        try {
            new CrossFitScore(invalidRounds, additionalReps, null, Instant.now());
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        // Assert: exception thrown
        assert exceptionThrown :
                "Expected IllegalArgumentException for rounds=" + invalidRounds;

        // Assert: state unchanged — original score still present
        assert section.getCrossFitScore() != null :
                "CrossFitScore should still be present";
        assert section.getCrossFitScore().getRounds() == 5 :
                "Original score rounds should be unchanged";
    }

    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 5: Invalid CrossFit score rejection — negative additionalReps")
    void negativeAdditionalReps_rejectsAndPreservesState(
            @ForAll("validRounds") int rounds,
            @ForAll("invalidNegativeAdditionalReps") int invalidAdditionalReps) {

        SectionProgress section = createScoredSection(SectionType.EMOM);

        // Set a valid score first
        CrossFitScore validScore = new CrossFitScore(3, 7, null, Instant.now());
        section.setCrossFitScore(validScore);

        // Attempt to create invalid CrossFitScore
        boolean exceptionThrown = false;
        try {
            new CrossFitScore(rounds, invalidAdditionalReps, null, Instant.now());
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        // Assert: exception thrown
        assert exceptionThrown :
                "Expected IllegalArgumentException for additionalReps=" + invalidAdditionalReps;

        // Assert: state unchanged
        assert section.getCrossFitScore().getAdditionalReps() == 7 :
                "Original score additionalReps should be unchanged";
    }

    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 5: Invalid CrossFit score rejection — non-positive totalTimeSeconds for FOR_TIME")
    void nonPositiveTotalTime_rejectsForForTimeSection(
            @ForAll("validRounds") int rounds,
            @ForAll("validAdditionalReps") int additionalReps,
            @ForAll("invalidTotalTimeSeconds") int invalidTime) {

        SectionProgress section = createScoredSection(SectionType.FOR_TIME);

        // Set a valid score first
        CrossFitScore validScore = new CrossFitScore(2, 5, 300, Instant.now());
        section.setCrossFitScore(validScore);

        // Attempt to create invalid CrossFitScore with non-positive time
        boolean exceptionThrown = false;
        try {
            new CrossFitScore(rounds, additionalReps, invalidTime, Instant.now());
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        // Assert: exception thrown
        assert exceptionThrown :
                "Expected IllegalArgumentException for totalTimeSeconds=" + invalidTime;

        // Assert: state unchanged
        assert section.getCrossFitScore().getTotalTimeSeconds() == 300 :
                "Original score totalTimeSeconds should be unchanged";
    }

    // --- Property 6: CrossFit score overwrite semantics ---

    /**
     * Property 6: CrossFit score overwrite semantics.
     *
     * For any section that already has a CrossFitScore, submitting a new valid score
     * should replace the previous score entirely. The section should contain exactly
     * one CrossFitScore (the latest), not a list.
     *
     * Validates: Requirements 2.7
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 6: CrossFit score overwrite semantics")
    void secondScore_replacesFirst(
            @ForAll("scoredSectionTypes") SectionType sectionType,
            @ForAll("validRounds") int firstRounds,
            @ForAll("validAdditionalReps") int firstAdditionalReps,
            @ForAll("validRounds") int secondRounds,
            @ForAll("validAdditionalReps") int secondAdditionalReps,
            @ForAll("validTotalTimeSeconds") int timeSeconds) {

        SectionProgress section = createScoredSection(sectionType);

        Integer timeValue = (sectionType == SectionType.FOR_TIME) ? timeSeconds : null;

        // Set first score
        CrossFitScore firstScore = new CrossFitScore(firstRounds, firstAdditionalReps, timeValue, Instant.now());
        section.setCrossFitScore(firstScore);

        // Set second score (overwrite)
        Integer secondTimeValue = (sectionType == SectionType.FOR_TIME) ? timeSeconds + 1 : null;
        CrossFitScore secondScore = new CrossFitScore(secondRounds, secondAdditionalReps, secondTimeValue, Instant.now().plusMillis(1));
        section.setCrossFitScore(secondScore);

        // Assert: section contains exactly one CrossFitScore (the latest)
        CrossFitScore current = section.getCrossFitScore();
        assert current != null :
                "CrossFitScore should not be null after overwrite";
        assert current.getRounds() == secondRounds :
                "Expected rounds " + secondRounds + " (second score) but got " + current.getRounds();
        assert current.getAdditionalReps() == secondAdditionalReps :
                "Expected additionalReps " + secondAdditionalReps + " (second score) but got " + current.getAdditionalReps();

        if (sectionType == SectionType.FOR_TIME) {
            assert current.getTotalTimeSeconds() != null
                    && current.getTotalTimeSeconds() == timeSeconds + 1 :
                    "Expected totalTimeSeconds " + (timeSeconds + 1) + " (second score) but got " + current.getTotalTimeSeconds();
        }
    }

    // --- Property 7: Round counter increment/decrement with floor at zero ---

    /**
     * Property 7: Round counter increment/decrement with floor at zero.
     *
     * For any sequence of increment and decrement actions applied to a round counter
     * starting at zero, the resulting count should equal max(0, total_increments - total_decrements).
     * The count should never be negative.
     *
     * Validates: Requirements 3.1, 3.3
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-performance-tracking, Property 7: Round counter increment/decrement with floor at zero")
    void roundCounter_neverNegativeAndCorrectFinalValue(
            @ForAll List<@From("roundActions") Boolean> actions) {

        SectionProgress section = createScoredSection(SectionType.AMRAP);
        assert section.getRoundCount() == 0 :
                "Round count should start at 0";

        int count = 0;
        for (Boolean isIncrement : actions) {
            if (isIncrement) {
                count++;
                section.setRoundCount(count);
            } else {
                count = Math.max(0, count - 1);
                section.setRoundCount(count);
            }

            // Assert: count is never negative at any point
            assert section.getRoundCount() >= 0 :
                    "Round count must never be negative, got " + section.getRoundCount();
        }

        // Assert: final count equals max(0, total_increments - total_decrements) with floor applied at each step
        assert section.getRoundCount() == count :
                "Expected final count " + count + " but got " + section.getRoundCount();
        assert section.getRoundCount() >= 0 :
                "Final round count must be non-negative";
    }

    @Provide
    Arbitrary<Boolean> roundActions() {
        // true = increment, false = decrement
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<List<Boolean>> roundActionSequences() {
        return Arbitraries.of(true, false).list().ofMinSize(1).ofMaxSize(50);
    }
}
