package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property-based tests for the parse–format round trip.
 * <p>
 * Feature: workout-creator-service-mvp1, Property 5: Parse–format round trip
 * Validates: Requirements 2.1, 3.1, 3.2
 * <p>
 * For any valid Workout domain object, formatting it with WorkoutFormatter and then
 * parsing the resulting text with WorkoutParser shall produce a domain object equivalent
 * to the original. The same property holds for Program objects.
 */
class WorkoutParserFormatterPropertyTest {

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

    // ── Workout generators ────────────────────────────────────────────

    @Provide
    Arbitrary<String> workoutNames() {
        return Arbitraries.of(
                "Morning Grind", "Leg Day", "Upper Body Blast", "Full Body Burn",
                "Push Day", "Pull Day", "Conditioning Session", "Strength Focus");
    }

    @Provide
    Arbitrary<String> descriptions() {
        return Arbitraries.of(
                "A heavy strength session", "High intensity conditioning",
                "Hypertrophy focused upper body", "Full body functional training",
                "Quick metabolic finisher", "Olympic lifting technique work",
                "Accessory and isolation work", "Mixed modal CrossFit workout");
    }

    @Provide
    Arbitrary<Workout> validWorkouts() {
        return Combinators.combine(
                workoutNames(),
                descriptions(),
                Arbitraries.of(TrainingStyle.values()),
                anyValidSection().list().ofMinSize(1).ofMaxSize(4)
        ).as(Workout::new);
    }

    @Provide
    Arbitrary<Program> validPrograms() {
        // Programs need at least 1 workout; use small sizes for test speed
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
                workoutNames(),
                descriptions(),
                Arbitraries.of(TrainingStyle.values()),
                anyValidSection().list().ofMinSize(1).ofMaxSize(2)
        ).as(Workout::new);
    }

    // ── Round-trip properties ─────────────────────────────────────────

    // Feature: workout-creator-service-mvp1, Property 5: Parse-format round trip
    @Property(tries = 100)
    void formatThenParseShouldRoundTripForWorkout(
            @ForAll("validWorkouts") Workout original) throws ParsingException {

        String formatted = WorkoutFormatter.format(original);
        Workout parsed = WorkoutParser.parseWorkout(formatted, GenerationScope.DAY);

        assertEquals(original, parsed,
                "parse(format(workout)) must equal the original workout.\n"
                        + "Original: " + original + "\n"
                        + "Formatted text:\n" + formatted + "\n"
                        + "Parsed: " + parsed);
    }

    // Feature: workout-creator-service-mvp1, Property 5: Parse-format round trip
    @Property(tries = 100)
    void formatThenParseShouldRoundTripForProgram(
            @ForAll("validPrograms") Program original) throws ParsingException {

        String formatted = WorkoutFormatter.format(original);
        Program parsed = WorkoutParser.parseProgram(formatted, original.getScope());

        assertEquals(original, parsed,
                "parse(format(program)) must equal the original program.\n"
                        + "Original: " + original + "\n"
                        + "Formatted text:\n" + formatted + "\n"
                        + "Parsed: " + parsed);
    }

    // Feature: workout-creator-service-mvp1, Property 5: Parse-format round trip
    @Property(tries = 100)
    void doubleRoundTripProducesSameTextForWorkout(
            @ForAll("validWorkouts") Workout original) throws ParsingException {

        String firstFormat = WorkoutFormatter.format(original);
        Workout parsed = WorkoutParser.parseWorkout(firstFormat, GenerationScope.DAY);
        String secondFormat = WorkoutFormatter.format(parsed);

        assertEquals(firstFormat, secondFormat,
                "format(parse(format(workout))) must produce identical text.\n"
                        + "First format:\n" + firstFormat + "\n"
                        + "Second format:\n" + secondFormat);
    }

    // Feature: workout-creator-service-mvp1, Property 5: Parse-format round trip
    @Property(tries = 100)
    void doubleRoundTripProducesSameTextForProgram(
            @ForAll("validPrograms") Program original) throws ParsingException {

        String firstFormat = WorkoutFormatter.format(original);
        Program parsed = WorkoutParser.parseProgram(firstFormat, original.getScope());
        String secondFormat = WorkoutFormatter.format(parsed);

        assertEquals(firstFormat, secondFormat,
                "format(parse(format(program))) must produce identical text.\n"
                        + "First format:\n" + firstFormat + "\n"
                        + "Second format:\n" + secondFormat);
    }
}
