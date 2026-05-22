package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;

import net.jqwik.api.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Property-based tests for Navigation and completion logic.
 * Tests Properties 10–11 from the design document.
 */
class NavigationPropertyTest {

    private static final String USER_ID = "user-prop-test";
    private static final String WORKOUT_SNAPSHOT = "{\"sections\":[]}";

    // --- Generators ---

    @Provide
    Arbitrary<SectionType> sectionTypes() {
        return Arbitraries.of(SectionType.values());
    }

    @Provide
    Arbitrary<Session> sessionsWithVaryingCompletion() {
        return Arbitraries.integers().between(1, 5).flatMap(numSections ->
                Arbitraries.integers().between(1, 8).flatMap(exercisesPerSection -> {
                    int totalExercises = numSections * exercisesPerSection;
                    return Arbitraries.integers().between(0, (1 << Math.min(totalExercises, 20)) - 1)
                            .map(completionBits -> {
                                List<SectionProgress> sections = new ArrayList<>();
                                for (int i = 0; i < numSections; i++) {
                                    List<ExerciseLog> logs = new ArrayList<>();
                                    for (int e = 0; e < exercisesPerSection; e++) {
                                        logs.add(new ExerciseLog(e, "Exercise " + (e + 1)));
                                    }
                                    sections.add(new SectionProgress(i, "Section " + (i + 1), SectionType.STRENGTH, logs));
                                }

                                Session session = Session.start(UUID.randomUUID(), USER_ID, UUID.randomUUID(),
                                        UUID.randomUUID(), 1, 1, sections, WORKOUT_SNAPSHOT, Instant.now());

                                int bitIdx = 0;
                                Instant now = Instant.now();
                                for (int s = 0; s < numSections; s++) {
                                    for (int e = 0; e < exercisesPerSection; e++) {
                                        if (bitIdx < 20 && ((completionBits >> bitIdx) & 1) == 1) {
                                            session.completeExercise(s, e, now);
                                        }
                                        bitIdx++;
                                    }
                                }
                                return session;
                            });
                })
        );
    }

    // --- Property 10: Section navigator disabled state ---

    /**
     * Property 10: For any total section count N (1–10) and any current section index I (0 ≤ I < N),
     * the "previous" button should be disabled iff I = 0, and the "next" button should be disabled
     * iff I = N - 1.
     *
     * This test creates a real Session domain object, navigates to the given index, and verifies
     * that the navigation boundary conditions hold by checking whether advancing beyond bounds
     * throws an exception (disabled) or succeeds (enabled).
     *
     * **Validates: Requirements 4.4**
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-active-workout, Property 10: Section navigator disabled state")
    void sectionNavigator_disabledStateMatchesBoundaryConditions(
            @ForAll @net.jqwik.api.constraints.IntRange(min = 1, max = 10) int totalSections,
            @ForAll @net.jqwik.api.constraints.IntRange(min = 0, max = 9) int currentIndex) {

        // Skip invalid combinations where currentIndex >= totalSections
        Assume.that(currentIndex < totalSections);

        // Create a session with the given number of sections
        List<SectionProgress> sections = new ArrayList<>();
        for (int i = 0; i < totalSections; i++) {
            List<ExerciseLog> logs = List.of(new ExerciseLog(0, "Exercise 1"));
            sections.add(new SectionProgress(i, "Section " + (i + 1), SectionType.STRENGTH, logs));
        }

        Session session = Session.start(UUID.randomUUID(), USER_ID, UUID.randomUUID(),
                UUID.randomUUID(), 1, 1, sections, WORKOUT_SNAPSHOT, Instant.now());

        // Navigate to the target index
        if (currentIndex > 0) {
            session.advanceSection(currentIndex);
        }

        // Determine disabled states based on the domain rule
        boolean prevShouldBeDisabled = (currentIndex == 0);
        boolean nextShouldBeDisabled = (currentIndex == totalSections - 1);

        // Verify previous navigation: attempting to go to index -1 should be invalid (disabled)
        boolean prevNavigationBlocked = false;
        try {
            session.advanceSection(currentIndex - 1);
        } catch (IllegalArgumentException e) {
            prevNavigationBlocked = true;
        }
        // If at index 0, navigating to -1 must throw (prev is disabled)
        // If not at index 0, navigating to currentIndex-1 must succeed (prev is enabled)
        assert prevShouldBeDisabled == prevNavigationBlocked :
                "Previous disabled state mismatch: at index=" + currentIndex +
                        " of " + totalSections + " sections, expected prevDisabled=" +
                        prevShouldBeDisabled + " but navigation " +
                        (prevNavigationBlocked ? "was blocked" : "succeeded");

        // Reset to original position for next check
        session.advanceSection(currentIndex);

        // Verify next navigation: attempting to go to totalSections should be invalid (disabled)
        boolean nextNavigationBlocked = false;
        try {
            session.advanceSection(currentIndex + 1);
        } catch (IllegalArgumentException e) {
            nextNavigationBlocked = true;
        }
        // If at last index, navigating to totalSections must throw (next is disabled)
        // If not at last index, navigating to currentIndex+1 must succeed (next is enabled)
        assert nextShouldBeDisabled == nextNavigationBlocked :
                "Next disabled state mismatch: at index=" + currentIndex +
                        " of " + totalSections + " sections, expected nextDisabled=" +
                        nextShouldBeDisabled + " but navigation " +
                        (nextNavigationBlocked ? "was blocked" : "succeeded");
    }

    // --- Property 11: All-complete predicate ---

    /**
     * **Validates: Requirements 4.10**
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-active-workout, Property 11: All-complete predicate")
    void allCompletePredicate_trueIffEveryExerciseCompleted(
            @ForAll("sessionsWithVaryingCompletion") Session session) {

        boolean allComplete = session.isAllComplete();

        // Manually check: every exercise in every section must be completed
        boolean expectedAllComplete = true;
        for (SectionProgress sp : session.getSectionProgresses()) {
            for (ExerciseLog log : sp.getExerciseLogs()) {
                if (!log.isCompleted()) {
                    expectedAllComplete = false;
                    break;
                }
            }
            if (!expectedAllComplete) break;
        }

        assert allComplete == expectedAllComplete :
                "isAllComplete() returned " + allComplete + " but expected " + expectedAllComplete;
    }
}
