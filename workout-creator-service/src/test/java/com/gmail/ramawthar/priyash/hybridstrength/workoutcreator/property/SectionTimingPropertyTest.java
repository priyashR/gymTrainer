package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.Exercise;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.Section;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.SectionType;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.List;

/**
 * Property-based tests for Section timing invariants.
 * <p>
 * Feature: workout-creator-service-mvp1, Property 8: Section timing fields match SectionType
 * Validates: Requirements 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8
 */
class SectionTimingPropertyTest {

    // ── Providers ─────────────────────────────────────────────────────

    @Provide
    Arbitrary<String> sectionNames() {
        return Arbitraries.of(
                "Strength Block", "AMRAP Finisher", "EMOM Work", "Tabata Blast",
                "For Time Challenge", "Accessory Work", "Main Lift", "Conditioning");
    }

    @Provide
    Arbitrary<List<Exercise>> timedExerciseList() {
        return timedExercise().list().ofMinSize(1).ofMaxSize(8);
    }

    @Provide
    Arbitrary<List<Exercise>> restBasedExerciseList() {
        return restBasedExercise().list().ofMinSize(1).ofMaxSize(8);
    }

    // ── Exercise generators (not @Provide — used internally) ──────────

    private Arbitrary<Exercise> timedExercise() {
        return Combinators.combine(
                exerciseNames(),
                Arbitraries.integers().between(1, 10),
                repSchemes(),
                weightStrings()
        ).as((name, sets, reps, weight) -> new Exercise(name, sets, reps, weight, null));
    }

    private Arbitrary<Exercise> restBasedExercise() {
        return Combinators.combine(
                exerciseNames(),
                Arbitraries.integers().between(1, 10),
                repSchemes(),
                weightStrings(),
                Arbitraries.integers().between(30, 300)
        ).as(Exercise::new);
    }

    private Arbitrary<String> exerciseNames() {
        return Arbitraries.of(
                "Back Squat", "Deadlift", "Bench Press", "Overhead Press",
                "Pull-Up", "Box Jump", "Burpee", "Kettlebell Swing",
                "Thruster", "Wall Ball", "Row", "Bike Erg");
    }

    private Arbitrary<String> repSchemes() {
        return Arbitraries.of("5", "8", "10", "12", "8-12", "max", "15", "20");
    }

    private Arbitrary<String> weightStrings() {
        return Arbitraries.of("135 lbs", "225 lbs", "bodyweight", "95 lbs", "50 kg", null);
    }

    // ── AMRAP property ────────────────────────────────────────────────

    // Feature: workout-creator-service-mvp1, Property 8: Section timing fields match SectionType
    @Property(tries = 100)
    void amrapSectionHasTimeCapAndNoExerciseRest(
            @ForAll("sectionNames") String name,
            @ForAll("timedExerciseList") List<Exercise> exercises,
            @ForAll @IntRange(min = 1, max = 60) int timeCapMinutes) {

        Section section = new Section(name, SectionType.AMRAP, exercises,
                timeCapMinutes, null, null, null, null);

        assertTimedSectionInvariants(section, SectionType.AMRAP);
        assertPositive(section.getTimeCapMinutes(), "AMRAP timeCapMinutes");
    }

    // ── FOR_TIME property ─────────────────────────────────────────────

    // Feature: workout-creator-service-mvp1, Property 8: Section timing fields match SectionType
    @Property(tries = 100)
    void forTimeSectionHasTimeCapAndNoExerciseRest(
            @ForAll("sectionNames") String name,
            @ForAll("timedExerciseList") List<Exercise> exercises,
            @ForAll @IntRange(min = 1, max = 60) int timeCapMinutes) {

        Section section = new Section(name, SectionType.FOR_TIME, exercises,
                timeCapMinutes, null, null, null, null);

        assertTimedSectionInvariants(section, SectionType.FOR_TIME);
        assertPositive(section.getTimeCapMinutes(), "FOR_TIME timeCapMinutes");
    }

    // ── EMOM property ─────────────────────────────────────────────────

    // Feature: workout-creator-service-mvp1, Property 8: Section timing fields match SectionType
    @Property(tries = 100)
    void emomSectionHasIntervalAndRoundsAndNoExerciseRest(
            @ForAll("sectionNames") String name,
            @ForAll("timedExerciseList") List<Exercise> exercises,
            @ForAll @IntRange(min = 10, max = 120) int intervalSeconds,
            @ForAll @IntRange(min = 1, max = 30) int totalRounds) {

        Section section = new Section(name, SectionType.EMOM, exercises,
                null, intervalSeconds, totalRounds, null, null);

        assertTimedSectionInvariants(section, SectionType.EMOM);
        assertPositive(section.getIntervalSeconds(), "EMOM intervalSeconds");
        assertPositive(section.getTotalRounds(), "EMOM totalRounds");
    }

    // ── TABATA property ───────────────────────────────────────────────

    // Feature: workout-creator-service-mvp1, Property 8: Section timing fields match SectionType
    @Property(tries = 100)
    void tabataSectionHasWorkRestRoundsAndNoExerciseRest(
            @ForAll("sectionNames") String name,
            @ForAll("timedExerciseList") List<Exercise> exercises,
            @ForAll @IntRange(min = 10, max = 60) int workIntervalSeconds,
            @ForAll @IntRange(min = 5, max = 30) int restIntervalSeconds,
            @ForAll @IntRange(min = 1, max = 20) int totalRounds) {

        Section section = new Section(name, SectionType.TABATA, exercises,
                null, null, totalRounds, workIntervalSeconds, restIntervalSeconds);

        assertTimedSectionInvariants(section, SectionType.TABATA);
        assertPositive(section.getWorkIntervalSeconds(), "TABATA workIntervalSeconds");
        assertPositive(section.getRestIntervalSeconds(), "TABATA restIntervalSeconds");
        assertPositive(section.getTotalRounds(), "TABATA totalRounds");
    }

    // ── STRENGTH property ─────────────────────────────────────────────

    // Feature: workout-creator-service-mvp1, Property 8: Section timing fields match SectionType
    @Property(tries = 100)
    void strengthSectionHasExerciseRestAndNoTimedFields(
            @ForAll("sectionNames") String name,
            @ForAll("restBasedExerciseList") List<Exercise> exercises) {

        Section section = new Section(name, SectionType.STRENGTH, exercises,
                null, null, null, null, null);

        assertRestBasedSectionInvariants(section, SectionType.STRENGTH);
    }

    // ── ACCESSORY property ────────────────────────────────────────────

    // Feature: workout-creator-service-mvp1, Property 8: Section timing fields match SectionType
    @Property(tries = 100)
    void accessorySectionHasExerciseRestAndNoTimedFields(
            @ForAll("sectionNames") String name,
            @ForAll("restBasedExerciseList") List<Exercise> exercises) {

        Section section = new Section(name, SectionType.ACCESSORY, exercises,
                null, null, null, null, null);

        assertRestBasedSectionInvariants(section, SectionType.ACCESSORY);
    }

    // ── Rejection properties ──────────────────────────────────────────

    // Feature: workout-creator-service-mvp1, Property 8: Section timing fields match SectionType
    @Property(tries = 100)
    void timedSectionRejectsExercisesWithRestSeconds(
            @ForAll("sectionNames") String name,
            @ForAll("restBasedExerciseList") List<Exercise> exercises,
            @ForAll @IntRange(min = 1, max = 60) int timeCapMinutes) {

        for (SectionType timedType : List.of(SectionType.AMRAP, SectionType.FOR_TIME)) {
            try {
                new Section(name, timedType, exercises,
                        timeCapMinutes, null, null, null, null);
                throw new AssertionError(timedType + " should reject exercises with restSeconds");
            } catch (IllegalArgumentException expected) {
                // correct — invariant enforced
            }
        }
    }

    // Feature: workout-creator-service-mvp1, Property 8: Section timing fields match SectionType
    @Property(tries = 100)
    void restBasedSectionRejectsExercisesWithoutRestSeconds(
            @ForAll("sectionNames") String name,
            @ForAll("timedExerciseList") List<Exercise> exercises) {

        for (SectionType restType : List.of(SectionType.STRENGTH, SectionType.ACCESSORY)) {
            try {
                new Section(name, restType, exercises,
                        null, null, null, null, null);
                throw new AssertionError(restType + " should reject exercises without restSeconds");
            } catch (IllegalArgumentException expected) {
                // correct — invariant enforced
            }
        }
    }

    // ── Assertion helpers ─────────────────────────────────────────────

    private void assertPositive(Integer value, String fieldName) {
        assert value != null && value > 0 : fieldName + " must be non-null and positive, got: " + value;
    }

    private void assertTimedSectionInvariants(Section section, SectionType expectedType) {
        assert section.getType() == expectedType;

        for (Exercise ex : section.getExercises()) {
            assert ex.getRestSeconds() == null
                    : expectedType + " exercise must not have restSeconds, but '"
                    + ex.getName() + "' has: " + ex.getRestSeconds();
        }

        if (expectedType == SectionType.AMRAP || expectedType == SectionType.FOR_TIME) {
            assert section.getIntervalSeconds() == null : "intervalSeconds must be null for " + expectedType;
            assert section.getTotalRounds() == null : "totalRounds must be null for " + expectedType;
            assert section.getWorkIntervalSeconds() == null : "workIntervalSeconds must be null for " + expectedType;
            assert section.getRestIntervalSeconds() == null : "restIntervalSeconds must be null for " + expectedType;
        } else if (expectedType == SectionType.EMOM) {
            assert section.getTimeCapMinutes() == null : "timeCapMinutes must be null for EMOM";
            assert section.getWorkIntervalSeconds() == null : "workIntervalSeconds must be null for EMOM";
            assert section.getRestIntervalSeconds() == null : "restIntervalSeconds must be null for EMOM";
        } else if (expectedType == SectionType.TABATA) {
            assert section.getTimeCapMinutes() == null : "timeCapMinutes must be null for TABATA";
            assert section.getIntervalSeconds() == null : "intervalSeconds must be null for TABATA";
        }
    }

    private void assertRestBasedSectionInvariants(Section section, SectionType expectedType) {
        assert section.getType() == expectedType;

        assert section.getTimeCapMinutes() == null : "timeCapMinutes must be null for " + expectedType;
        assert section.getIntervalSeconds() == null : "intervalSeconds must be null for " + expectedType;
        assert section.getTotalRounds() == null : "totalRounds must be null for " + expectedType;
        assert section.getWorkIntervalSeconds() == null : "workIntervalSeconds must be null for " + expectedType;
        assert section.getRestIntervalSeconds() == null : "restIntervalSeconds must be null for " + expectedType;

        for (Exercise ex : section.getExercises()) {
            assert ex.getRestSeconds() != null && ex.getRestSeconds() > 0
                    : expectedType + " exercise must have positive restSeconds, but '"
                    + ex.getName() + "' has: " + ex.getRestSeconds();
        }
    }
}
