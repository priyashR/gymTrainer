package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.application.GenerationService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.ports.outbound.GeminiClient;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for {@link GenerationService}.
 * <p>
 * Feature: workout-creator-service-mvp1
 * <ul>
 *   <li>Property 1: Scope determines result type</li>
 *   <li>Property 9: Successful generation result structure</li>
 *   <li>Property 10: Failed parse result structure</li>
 * </ul>
 * Validates: Requirements 1.1, 1.5, 5.1, 5.2, 5.3, 5.4
 * <p>
 * No Spring context — plain Java instantiation with Mockito mocks.
 */
class GenerationPropertyTest {

    // ── Exercise generators ───────────────────────────────────────────

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
        return Arbitraries.of("135 lbs", "225 lbs", "bodyweight", "95 lbs", "50 kg");
    }

    private Arbitrary<Exercise> timedExercise() {
        return Combinators.combine(
                exerciseNames(),
                Arbitraries.integers().between(1, 10),
                repSchemes(),
                Arbitraries.oneOf(weightStrings(), Arbitraries.just(null))
        ).as((name, sets, reps, weight) -> new Exercise(name, sets, reps, weight, null));
    }

    private Arbitrary<Exercise> restBasedExercise() {
        return Combinators.combine(
                exerciseNames(),
                Arbitraries.integers().between(1, 10),
                repSchemes(),
                Arbitraries.oneOf(weightStrings(), Arbitraries.just(null)),
                Arbitraries.integers().between(30, 300)
        ).as(Exercise::new);
    }

    // ── Section generators ────────────────────────────────────────────

    private Arbitrary<String> sectionNames() {
        return Arbitraries.of(
                "Strength Block", "AMRAP Finisher", "EMOM Work", "Tabata Blast",
                "For Time Challenge", "Accessory Work", "Main Lift", "Conditioning");
    }

    private Arbitrary<Section> strengthSection() {
        return Combinators.combine(
                sectionNames(),
                restBasedExercise().list().ofMinSize(1).ofMaxSize(4)
        ).as((name, exercises) ->
                new Section(name, SectionType.STRENGTH, exercises, null, null, null, null, null));
    }

    private Arbitrary<Section> amrapSection() {
        return Combinators.combine(
                sectionNames(),
                timedExercise().list().ofMinSize(1).ofMaxSize(4),
                Arbitraries.integers().between(1, 60)
        ).as((name, exercises, timeCap) ->
                new Section(name, SectionType.AMRAP, exercises, timeCap, null, null, null, null));
    }

    private Arbitrary<Section> anyValidSection() {
        return Arbitraries.oneOf(strengthSection(), amrapSection());
    }

    // ── Workout / Program generators ──────────────────────────────────

    private Arbitrary<String> workoutNames() {
        return Arbitraries.of(
                "Morning Grind", "Leg Day", "Upper Body Blast", "Full Body Burn",
                "Push Day", "Pull Day", "Conditioning Session", "Strength Focus");
    }

    private Arbitrary<String> descriptions() {
        return Arbitraries.of(
                "A heavy strength session", "High intensity conditioning",
                "Hypertrophy focused upper body", "Full body functional training");
    }

    @Provide
    Arbitrary<Workout> validWorkouts() {
        return Combinators.combine(
                workoutNames(),
                descriptions(),
                Arbitraries.of(TrainingStyle.values()),
                anyValidSection().list().ofMinSize(1).ofMaxSize(3)
        ).as(Workout::new);
    }

    private Arbitrary<Program> programsWithScope(GenerationScope scope) {
        return Combinators.combine(
                Arbitraries.of("Week Plan", "Monthly Program", "Strength Cycle"),
                Arbitraries.of("A structured training program", "Progressive overload plan"),
                Arbitraries.of(TrainingStyle.values()).list().ofMinSize(1).ofMaxSize(3).uniqueElements(),
                validWorkoutForProgram().list().ofMinSize(2).ofMaxSize(4)
        ).as((name, desc, styles, workouts) -> new Program(name, desc, scope, styles, workouts));
    }

    @Provide
    Arbitrary<Program> weekPrograms() {
        return programsWithScope(GenerationScope.WEEK);
    }

    @Provide
    Arbitrary<Program> fourWeekPrograms() {
        return programsWithScope(GenerationScope.FOUR_WEEK);
    }

    private Arbitrary<Workout> validWorkoutForProgram() {
        return Combinators.combine(
                workoutNames(),
                descriptions(),
                Arbitraries.of(TrainingStyle.values()),
                anyValidSection().list().ofMinSize(1).ofMaxSize(2)
        ).as(Workout::new);
    }

    // ── Command generators ────────────────────────────────────────────

    @Provide
    Arbitrary<GenerationCommand> dayCommands() {
        return Combinators.combine(
                Arbitraries.create(UUID::randomUUID),
                descriptions(),
                Arbitraries.of(TrainingStyle.values())
        ).as((userId, desc, style) ->
                new GenerationCommand(userId, desc, GenerationScope.DAY, List.of(style)));
    }

    @Provide
    Arbitrary<GenerationCommand> weekCommands() {
        return Combinators.combine(
                Arbitraries.create(UUID::randomUUID),
                descriptions(),
                Arbitraries.of(TrainingStyle.values()).list().ofMinSize(1).ofMaxSize(3).uniqueElements()
        ).as((userId, desc, styles) ->
                new GenerationCommand(userId, desc, GenerationScope.WEEK, styles));
    }

    @Provide
    Arbitrary<GenerationCommand> fourWeekCommands() {
        return Combinators.combine(
                Arbitraries.create(UUID::randomUUID),
                descriptions(),
                Arbitraries.of(TrainingStyle.values()).list().ofMinSize(1).ofMaxSize(3).uniqueElements()
        ).as((userId, desc, styles) ->
                new GenerationCommand(userId, desc, GenerationScope.FOUR_WEEK, styles));
    }

    @Provide
    Arbitrary<String> unparseableResponses() {
        return Arbitraries.of(
                "This is not a valid workout format at all.",
                "Random gibberish text with no structure",
                "WORKOUT: Missing everything else",
                "Just some plain text response from the AI",
                "{ \"json\": \"but not our format\" }",
                "ERROR: something went wrong on the AI side",
                "",
                "   ",
                "WORKOUT:\nDESCRIPTION:\nTRAINING_STYLE: INVALID_STYLE");
    }

    // ── Helper: build a GenerationService with a mocked GeminiClient ──

    private GenerationService serviceReturning(String geminiResponse) {
        GeminiClient mockClient = mock(GeminiClient.class);
        when(mockClient.generate(anyString())).thenReturn(geminiResponse);
        return new GenerationService(mockClient);
    }

    // ── Property 1: Scope determines result type ──────────────────────

    // Feature: workout-creator-service-mvp1, Property 1: Scope determines result type
    @Property(tries = 100)
    void dayScopeProducesWorkoutAndNullProgram(
            @ForAll("dayCommands") GenerationCommand command,
            @ForAll("validWorkouts") Workout workout) {

        String formatted = WorkoutFormatter.format(workout);
        GenerationService service = serviceReturning(formatted);

        GenerationResult result = service.generate(command);

        assertNotNull(result.getWorkout(),
                "DAY scope must produce a non-null workout");
        assertNull(result.getProgram(),
                "DAY scope must produce a null program");
    }

    // Feature: workout-creator-service-mvp1, Property 1: Scope determines result type
    @Property(tries = 100)
    void weekScopeProducesProgramAndNullWorkout(
            @ForAll("weekCommands") GenerationCommand command,
            @ForAll("weekPrograms") Program program) {

        String formatted = WorkoutFormatter.format(program);
        GenerationService service = serviceReturning(formatted);

        GenerationResult result = service.generate(command);

        assertNull(result.getWorkout(),
                "WEEK scope must produce a null workout");
        assertNotNull(result.getProgram(),
                "WEEK scope must produce a non-null program");
    }

    // Feature: workout-creator-service-mvp1, Property 1: Scope determines result type
    @Property(tries = 100)
    void fourWeekScopeProducesProgramAndNullWorkout(
            @ForAll("fourWeekCommands") GenerationCommand command,
            @ForAll("fourWeekPrograms") Program program) {

        String formatted = WorkoutFormatter.format(program);
        GenerationService service = serviceReturning(formatted);

        GenerationResult result = service.generate(command);

        assertNull(result.getWorkout(),
                "FOUR_WEEK scope must produce a null workout");
        assertNotNull(result.getProgram(),
                "FOUR_WEEK scope must produce a non-null program");
    }

    // ── Property 9: Successful generation result structure ────────────

    // Feature: workout-creator-service-mvp1, Property 9: Successful generation result structure
    @Property(tries = 100)
    void successfulDayResultHasRawTextParsedWorkoutAndNullError(
            @ForAll("dayCommands") GenerationCommand command,
            @ForAll("validWorkouts") Workout workout) {

        String formatted = WorkoutFormatter.format(workout);
        GenerationService service = serviceReturning(formatted);

        GenerationResult result = service.generate(command);

        assertNotNull(result.getRawGeminiResponse(),
                "Successful result must have non-null rawGeminiResponse");
        assertFalse(result.getRawGeminiResponse().isBlank(),
                "Successful result rawGeminiResponse must not be blank");
        assertNotNull(result.getWorkout(),
                "Successful DAY result must have non-null workout");
        assertNull(result.getParsingError(),
                "Successful result must have null parsingError");
    }

    // Feature: workout-creator-service-mvp1, Property 9: Successful generation result structure
    @Property(tries = 100)
    void successfulProgramResultHasRawTextParsedProgramAndNullError(
            @ForAll("weekCommands") GenerationCommand command,
            @ForAll("weekPrograms") Program program) {

        String formatted = WorkoutFormatter.format(program);
        GenerationService service = serviceReturning(formatted);

        GenerationResult result = service.generate(command);

        assertNotNull(result.getRawGeminiResponse(),
                "Successful result must have non-null rawGeminiResponse");
        assertFalse(result.getRawGeminiResponse().isBlank(),
                "Successful result rawGeminiResponse must not be blank");
        assertNotNull(result.getProgram(),
                "Successful WEEK/FOUR_WEEK result must have non-null program");
        assertNull(result.getParsingError(),
                "Successful result must have null parsingError");
    }

    // Feature: workout-creator-service-mvp1, Property 9: Successful generation result structure
    @Property(tries = 100)
    void successfulResultRawResponseMatchesGeminiOutput(
            @ForAll("dayCommands") GenerationCommand command,
            @ForAll("validWorkouts") Workout workout) {

        String formatted = WorkoutFormatter.format(workout);
        GenerationService service = serviceReturning(formatted);

        GenerationResult result = service.generate(command);

        assertEquals(formatted, result.getRawGeminiResponse(),
                "rawGeminiResponse must match the original Gemini output");
    }

    // ── Property 10: Failed parse result structure ────────────────────

    // Feature: workout-creator-service-mvp1, Property 10: Failed parse result structure
    @Property(tries = 100)
    void failedParseResultHasRawTextNullObjectsAndNonNullError(
            @ForAll("dayCommands") GenerationCommand command,
            @ForAll("unparseableResponses") String badResponse) {

        // Skip blank responses — GenerationResult rejects blank rawGeminiResponse
        Assume.that(badResponse != null && !badResponse.isBlank());

        GenerationService service = serviceReturning(badResponse);

        GenerationResult result = service.generate(command);

        assertNotNull(result.getRawGeminiResponse(),
                "Failed parse result must have non-null rawGeminiResponse");
        assertEquals(badResponse, result.getRawGeminiResponse(),
                "Failed parse result rawGeminiResponse must match Gemini output");
        assertNull(result.getWorkout(),
                "Failed parse result must have null workout");
        assertNull(result.getProgram(),
                "Failed parse result must have null program");
        assertNotNull(result.getParsingError(),
                "Failed parse result must have non-null parsingError");
        assertFalse(result.getParsingError().isBlank(),
                "Failed parse parsingError must contain a human-readable message");
    }

    // Feature: workout-creator-service-mvp1, Property 10: Failed parse result structure
    @Property(tries = 100)
    void failedParseProgramResultHasRawTextNullObjectsAndNonNullError(
            @ForAll("weekCommands") GenerationCommand command,
            @ForAll("unparseableResponses") String badResponse) {

        Assume.that(badResponse != null && !badResponse.isBlank());

        GenerationService service = serviceReturning(badResponse);

        GenerationResult result = service.generate(command);

        assertNotNull(result.getRawGeminiResponse(),
                "Failed parse result must have non-null rawGeminiResponse");
        assertEquals(badResponse, result.getRawGeminiResponse(),
                "Failed parse result rawGeminiResponse must match Gemini output");
        assertNull(result.getWorkout(),
                "Failed parse result must have null workout");
        assertNull(result.getProgram(),
                "Failed parse result must have null program");
        assertNotNull(result.getParsingError(),
                "Failed parse result must have non-null parsingError");
        assertFalse(result.getParsingError().isBlank(),
                "Failed parse parsingError must contain a human-readable message");
    }

    // Feature: workout-creator-service-mvp1, Property 10: Failed parse result structure
    @Property(tries = 100)
    void failedParseDoesNotThrowException(
            @ForAll("dayCommands") GenerationCommand command,
            @ForAll("unparseableResponses") String badResponse) {

        Assume.that(badResponse != null && !badResponse.isBlank());

        GenerationService service = serviceReturning(badResponse);

        // Req 5.3: parse failure must NOT throw — returns graceful result
        assertDoesNotThrow(() -> service.generate(command),
                "Parse failure must not throw an exception — it returns a result with error info");
    }
}
