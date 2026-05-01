package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.*;

import net.jqwik.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property-based tests for formatted output completeness.
 * <p>
 * Feature: workout-creator-service-mvp1, Property 7: Formatted output contains all domain fields
 * Validates: Requirements 3.3
 * <p>
 * For any valid Workout domain object, the text produced by WorkoutFormatter shall contain
 * every Section name, every Section's SectionType name, every Exercise name, and every
 * Exercise's sets, reps, weight (when non-null), and rest seconds (when non-null).
 */
class WorkoutFormatterPropertyTest {

    // ── Exercise generators ───────────────────────────────────────────

    private Arbitrary<String> exerciseNames() {
        return Arbitraries.of(
                "Back Squat", "Deadlift", "Bench Press", "Overhead Press",
                "Pull-Up", "Box Jump", "Burpee", "Kettlebell Swing",
                "Thruster", "Wall Ball", "Row", "Bike Erg",
                "Front Squat", "Barbell Curl", "Tricep Dip", "Lunge");
    }

    private Arbitrary<String> repSchemes() {
        return Arbitraries.of("5", "8", "10", "12", "8-12", "max", "15", "20", "3-5");
    }

    private Arbitrary<String> weightStrings() {
        return Arbitraries.of("135 lbs", "225 lbs", "bodyweight", "95 lbs", "50 kg",
                "60 percent", "bar only");
    }

    private Arbitrary<Exercise> timedExercise() {
        return Combinators.combine(
                exerciseNames(),
                Arbitraries.integers().between(1, 10),
                repSchemes(),
                Arbitraries.oneOf(weightStrings().map(w -> (String) w), Arbitraries.just(null))
        ).as((name, sets, reps, weight) -> new Exercise(name, sets, reps, weight, null));
    }

    private Arbitrary<Exercise> restBasedExercise() {
        return Combinators.combine(
                exerciseNames(),
                Arbitraries.integers().between(1, 10),
                repSchemes(),
                Arbitraries.oneOf(weightStrings().map(w -> (String) w), Arbitraries.just(null)),
                Arbitraries.integers().between(30, 300)
        ).as(Exercise::new);
    }

    // ── Section generators ────────────────────────────────────────────

    @Provide
    Arbitrary<String> sectionNames() {
        return Arbitraries.of(
                "Strength Block", "AMRAP Finisher", "EMOM Work", "Tabata Blast",
                "For Time Challenge", "Accessory Work", "Main Lift", "Conditioning");
    }

    private Arbitrary<Section> amrapSection() {
        return Combinators.combine(
                sectionNames(),
                timedExercise().list().ofMinSize(1).ofMaxSize(5),
                Arbitraries.integers().between(1, 60)
        ).as((name, exercises, timeCap) ->
                new Section(name, SectionType.AMRAP, exercises, timeCap, null, null, null, null));
    }

    private Arbitrary<Section> forTimeSection() {
        return Combinators.combine(
                sectionNames(),
                timedExercise().list().ofMinSize(1).ofMaxSize(5),
                Arbitraries.integers().between(1, 60)
        ).as((name, exercises, timeCap) ->
                new Section(name, SectionType.FOR_TIME, exercises, timeCap, null, null, null, null));
    }

    private Arbitrary<Section> emomSection() {
        return Combinators.combine(
                sectionNames(),
                timedExercise().list().ofMinSize(1).ofMaxSize(5),
                Arbitraries.integers().between(10, 120),
                Arbitraries.integers().between(1, 30)
        ).as((name, exercises, interval, rounds) ->
                new Section(name, SectionType.EMOM, exercises, null, interval, rounds, null, null));
    }

    private Arbitrary<Section> tabataSection() {
        return Combinators.combine(
                sectionNames(),
                timedExercise().list().ofMinSize(1).ofMaxSize(5),
                Arbitraries.integers().between(10, 60),
                Arbitraries.integers().between(5, 30),
                Arbitraries.integers().between(1, 20)
        ).as((name, exercises, work, rest, rounds) ->
                new Section(name, SectionType.TABATA, exercises, null, null, rounds, work, rest));
    }

    private Arbitrary<Section> strengthSection() {
        return Combinators.combine(
                sectionNames(),
                restBasedExercise().list().ofMinSize(1).ofMaxSize(5)
        ).as((name, exercises) ->
                new Section(name, SectionType.STRENGTH, exercises, null, null, null, null, null));
    }

    private Arbitrary<Section> accessorySection() {
        return Combinators.combine(
                sectionNames(),
                restBasedExercise().list().ofMinSize(1).ofMaxSize(5)
        ).as((name, exercises) ->
                new Section(name, SectionType.ACCESSORY, exercises, null, null, null, null, null));
    }

    private Arbitrary<Section> anyValidSection() {
        return Arbitraries.oneOf(
                amrapSection(), forTimeSection(), emomSection(),
                tabataSection(), strengthSection(), accessorySection());
    }

    // ── Workout generator ─────────────────────────────────────────────

    @Provide
    Arbitrary<Workout> validWorkouts() {
        return Combinators.combine(
                Arbitraries.of("Morning Grind", "Leg Day", "Upper Body Blast", "Full Body Burn",
                        "Push Day", "Pull Day", "Conditioning Session", "Strength Focus"),
                Arbitraries.of("A heavy strength session", "High intensity conditioning",
                        "Hypertrophy focused upper body", "Full body functional training",
                        "Quick metabolic finisher", "Olympic lifting technique work"),
                Arbitraries.of(TrainingStyle.values()),
                anyValidSection().list().ofMinSize(1).ofMaxSize(4)
        ).as(Workout::new);
    }

    // ── Program generator ─────────────────────────────────────────────

    @Provide
    Arbitrary<Program> validPrograms() {
        return Combinators.combine(
                Arbitraries.of("Week Plan", "Monthly Program", "Strength Cycle", "Hypertrophy Block"),
                Arbitraries.of("A structured training program", "Progressive overload plan",
                        "Periodised training block", "General fitness program"),
                Arbitraries.of(GenerationScope.WEEK, GenerationScope.FOUR_WEEK),
                Arbitraries.of(TrainingStyle.values()).list().ofMinSize(1).ofMaxSize(3).uniqueElements(),
                validWorkoutForProgram().list().ofMinSize(2).ofMaxSize(4)
        ).as(Program::new);
    }

    private Arbitrary<Workout> validWorkoutForProgram() {
        return Combinators.combine(
                Arbitraries.of("Morning Grind", "Leg Day", "Upper Body Blast", "Full Body Burn"),
                Arbitraries.of("A heavy strength session", "High intensity conditioning",
                        "Hypertrophy focused upper body", "Full body functional training"),
                Arbitraries.of(TrainingStyle.values()),
                anyValidSection().list().ofMinSize(1).ofMaxSize(2)
        ).as(Workout::new);
    }

    // ── Property: Workout formatted output contains all domain fields ─

    // Feature: workout-creator-service-mvp1, Property 7: Formatted output contains all domain fields
    @Property(tries = 100)
    void formattedWorkoutContainsAllSectionNames(
            @ForAll("validWorkouts") Workout workout) {

        String formatted = WorkoutFormatter.format(workout);

        for (Section section : workout.getSections()) {
            assertTrue(formatted.contains(section.getName()),
                    "Formatted output must contain section name '" + section.getName()
                            + "'\nFormatted:\n" + formatted);
        }
    }

    // Feature: workout-creator-service-mvp1, Property 7: Formatted output contains all domain fields
    @Property(tries = 100)
    void formattedWorkoutContainsAllSectionTypeNames(
            @ForAll("validWorkouts") Workout workout) {

        String formatted = WorkoutFormatter.format(workout);

        for (Section section : workout.getSections()) {
            assertTrue(formatted.contains(section.getType().name()),
                    "Formatted output must contain section type '" + section.getType().name()
                            + "'\nFormatted:\n" + formatted);
        }
    }

    // Feature: workout-creator-service-mvp1, Property 7: Formatted output contains all domain fields
    @Property(tries = 100)
    void formattedWorkoutContainsAllExerciseNames(
            @ForAll("validWorkouts") Workout workout) {

        String formatted = WorkoutFormatter.format(workout);

        for (Section section : workout.getSections()) {
            for (Exercise exercise : section.getExercises()) {
                assertTrue(formatted.contains(exercise.getName()),
                        "Formatted output must contain exercise name '" + exercise.getName()
                                + "'\nFormatted:\n" + formatted);
            }
        }
    }

    // Feature: workout-creator-service-mvp1, Property 7: Formatted output contains all domain fields
    @Property(tries = 100)
    void formattedWorkoutContainsAllExerciseSetsAndReps(
            @ForAll("validWorkouts") Workout workout) {

        String formatted = WorkoutFormatter.format(workout);

        for (Section section : workout.getSections()) {
            for (Exercise exercise : section.getExercises()) {
                assertTrue(formatted.contains("Sets: " + exercise.getSets()),
                        "Formatted output must contain sets '" + exercise.getSets()
                                + "' for exercise '" + exercise.getName()
                                + "'\nFormatted:\n" + formatted);
                assertTrue(formatted.contains("Reps: " + exercise.getReps()),
                        "Formatted output must contain reps '" + exercise.getReps()
                                + "' for exercise '" + exercise.getName()
                                + "'\nFormatted:\n" + formatted);
            }
        }
    }

    // Feature: workout-creator-service-mvp1, Property 7: Formatted output contains all domain fields
    @Property(tries = 100)
    void formattedWorkoutContainsWeightWhenPresent(
            @ForAll("validWorkouts") Workout workout) {

        String formatted = WorkoutFormatter.format(workout);

        for (Section section : workout.getSections()) {
            for (Exercise exercise : section.getExercises()) {
                if (exercise.getWeight() != null) {
                    assertTrue(formatted.contains("Weight: " + exercise.getWeight()),
                            "Formatted output must contain weight '" + exercise.getWeight()
                                    + "' for exercise '" + exercise.getName()
                                    + "'\nFormatted:\n" + formatted);
                }
            }
        }
    }

    // Feature: workout-creator-service-mvp1, Property 7: Formatted output contains all domain fields
    @Property(tries = 100)
    void formattedWorkoutContainsRestSecondsWhenPresent(
            @ForAll("validWorkouts") Workout workout) {

        String formatted = WorkoutFormatter.format(workout);

        for (Section section : workout.getSections()) {
            for (Exercise exercise : section.getExercises()) {
                if (exercise.getRestSeconds() != null) {
                    assertTrue(formatted.contains("Rest: " + exercise.getRestSeconds() + "s"),
                            "Formatted output must contain rest seconds '" + exercise.getRestSeconds()
                                    + "s' for exercise '" + exercise.getName()
                                    + "'\nFormatted:\n" + formatted);
                }
            }
        }
    }

    // ── Property: Program formatted output contains all domain fields ─

    // Feature: workout-creator-service-mvp1, Property 7: Formatted output contains all domain fields
    @Property(tries = 100)
    void formattedProgramContainsAllSectionNames(
            @ForAll("validPrograms") Program program) {

        String formatted = WorkoutFormatter.format(program);

        for (Workout workout : program.getWorkouts()) {
            for (Section section : workout.getSections()) {
                assertTrue(formatted.contains(section.getName()),
                        "Formatted program output must contain section name '" + section.getName()
                                + "'\nFormatted:\n" + formatted);
            }
        }
    }

    // Feature: workout-creator-service-mvp1, Property 7: Formatted output contains all domain fields
    @Property(tries = 100)
    void formattedProgramContainsAllSectionTypeNames(
            @ForAll("validPrograms") Program program) {

        String formatted = WorkoutFormatter.format(program);

        for (Workout workout : program.getWorkouts()) {
            for (Section section : workout.getSections()) {
                assertTrue(formatted.contains(section.getType().name()),
                        "Formatted program output must contain section type '" + section.getType().name()
                                + "'\nFormatted:\n" + formatted);
            }
        }
    }

    // Feature: workout-creator-service-mvp1, Property 7: Formatted output contains all domain fields
    @Property(tries = 100)
    void formattedProgramContainsAllExerciseFields(
            @ForAll("validPrograms") Program program) {

        String formatted = WorkoutFormatter.format(program);

        for (Workout workout : program.getWorkouts()) {
            for (Section section : workout.getSections()) {
                for (Exercise exercise : section.getExercises()) {
                    assertTrue(formatted.contains(exercise.getName()),
                            "Formatted program must contain exercise name '" + exercise.getName()
                                    + "'\nFormatted:\n" + formatted);
                    assertTrue(formatted.contains("Sets: " + exercise.getSets()),
                            "Formatted program must contain sets for '" + exercise.getName()
                                    + "'\nFormatted:\n" + formatted);
                    assertTrue(formatted.contains("Reps: " + exercise.getReps()),
                            "Formatted program must contain reps for '" + exercise.getName()
                                    + "'\nFormatted:\n" + formatted);
                    if (exercise.getWeight() != null) {
                        assertTrue(formatted.contains("Weight: " + exercise.getWeight()),
                                "Formatted program must contain weight for '" + exercise.getName()
                                        + "'\nFormatted:\n" + formatted);
                    }
                    if (exercise.getRestSeconds() != null) {
                        assertTrue(formatted.contains("Rest: " + exercise.getRestSeconds() + "s"),
                                "Formatted program must contain rest seconds for '" + exercise.getName()
                                        + "'\nFormatted:\n" + formatted);
                    }
                }
            }
        }
    }
}
