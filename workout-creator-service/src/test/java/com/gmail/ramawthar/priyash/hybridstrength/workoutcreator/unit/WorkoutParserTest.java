package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WorkoutParser}.
 * <p>
 * Tests specific parsing examples for each SectionType, malformed input,
 * empty input, and missing sections.
 * <p>
 * Naming convention: MethodName_StateUnderTest_ExpectedBehaviour
 * No Spring context — plain Java only.
 *
 * @see WorkoutParser
 * Requirements: 2.1, 2.2
 */
class WorkoutParserTest {

    // ---- Helper methods for building formatted text ----

    private static String strengthWorkoutText() {
        return """
                WORKOUT: Strength Day
                DESCRIPTION: Heavy compound lifts
                TRAINING_STYLE: STRENGTH
                --- SECTION: Main Lifts [TYPE: STRENGTH] ---
                - Back Squat | Sets: 5 | Reps: 5 | Weight: 225 lbs | Rest: 180s
                - Bench Press | Sets: 4 | Reps: 6 | Weight: 185 lbs | Rest: 120s""";
    }

    private static String amrapWorkoutText() {
        return """
                WORKOUT: AMRAP Burner
                DESCRIPTION: High intensity AMRAP session
                TRAINING_STYLE: CROSSFIT
                --- SECTION: AMRAP Block [TYPE: AMRAP] ---
                TIME_CAP_MINUTES: 12
                - Burpees | Sets: 1 | Reps: 10
                - Box Jumps | Sets: 1 | Reps: 15 | Weight: bodyweight""";
    }

    private static String emomWorkoutText() {
        return """
                WORKOUT: EMOM Session
                DESCRIPTION: Every minute on the minute
                TRAINING_STYLE: CROSSFIT
                --- SECTION: EMOM Block [TYPE: EMOM] ---
                INTERVAL_SECONDS: 60
                TOTAL_ROUNDS: 10
                - Power Clean | Sets: 1 | Reps: 3 | Weight: 135 lbs""";
    }

    private static String tabataWorkoutText() {
        return """
                WORKOUT: Tabata Blast
                DESCRIPTION: Tabata interval training
                TRAINING_STYLE: CROSSFIT
                --- SECTION: Tabata Round [TYPE: TABATA] ---
                WORK_INTERVAL_SECONDS: 20
                REST_INTERVAL_SECONDS: 10
                TOTAL_ROUNDS: 8
                - Air Squats | Sets: 1 | Reps: max
                - Push Ups | Sets: 1 | Reps: max""";
    }

    private static String forTimeWorkoutText() {
        return """
                WORKOUT: For Time WOD
                DESCRIPTION: Complete as fast as possible
                TRAINING_STYLE: CROSSFIT
                --- SECTION: The Workout [TYPE: FOR_TIME] ---
                TIME_CAP_MINUTES: 20
                - Thrusters | Sets: 1 | Reps: 21 | Weight: 95 lbs
                - Pull Ups | Sets: 1 | Reps: 21""";
    }

    private static String accessoryWorkoutText() {
        return """
                WORKOUT: Accessory Work
                DESCRIPTION: Isolation and accessory movements
                TRAINING_STYLE: HYPERTROPHY
                --- SECTION: Accessories [TYPE: ACCESSORY] ---
                - Bicep Curls | Sets: 3 | Reps: 12 | Weight: 30 lbs | Rest: 60s
                - Tricep Pushdowns | Sets: 3 | Reps: 15 | Weight: 40 lbs | Rest: 60s""";
    }

    private static String multiSectionWorkoutText() {
        return """
                WORKOUT: Full Session
                DESCRIPTION: Strength plus conditioning
                TRAINING_STYLE: CROSSFIT
                --- SECTION: Strength Block [TYPE: STRENGTH] ---
                - Deadlift | Sets: 5 | Reps: 3 | Weight: 315 lbs | Rest: 180s

                --- SECTION: Conditioning [TYPE: AMRAP] ---
                TIME_CAP_MINUTES: 10
                - Wall Balls | Sets: 1 | Reps: 20 | Weight: 20 lbs
                - Rowing | Sets: 1 | Reps: 250""";
    }

    private static String simpleProgramText() {
        return """
                PROGRAM: Weekly Plan
                DESCRIPTION: A simple weekly program
                SCOPE: WEEK
                TRAINING_STYLES: STRENGTH, CROSSFIT

                === DAY 1 ===
                WORKOUT: Day 1 Workout
                DESCRIPTION: Strength focus
                TRAINING_STYLE: STRENGTH
                --- SECTION: Main Lifts [TYPE: STRENGTH] ---
                - Squat | Sets: 5 | Reps: 5 | Weight: 200 lbs | Rest: 180s

                === DAY 2 ===
                WORKOUT: Day 2 Workout
                DESCRIPTION: Conditioning focus
                TRAINING_STYLE: CROSSFIT
                --- SECTION: WOD [TYPE: AMRAP] ---
                TIME_CAP_MINUTES: 15
                - Burpees | Sets: 1 | Reps: 10""";
    }

    // ========================================================================
    // Strength section parsing
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — STRENGTH section")
    class StrengthSectionParsing {

        @Test
        void parseWorkout_ValidStrengthSection_ParsesCorrectly() throws ParsingException {
            Workout workout = WorkoutParser.parseWorkout(strengthWorkoutText(), GenerationScope.DAY);

            assertEquals("Strength Day", workout.getName());
            assertEquals("Heavy compound lifts", workout.getDescription());
            assertEquals(TrainingStyle.STRENGTH, workout.getTrainingStyle());
            assertEquals(1, workout.getSections().size());

            Section section = workout.getSections().get(0);
            assertEquals("Main Lifts", section.getName());
            assertEquals(SectionType.STRENGTH, section.getType());
            assertNull(section.getTimeCapMinutes());
            assertNull(section.getIntervalSeconds());
            assertNull(section.getTotalRounds());

            assertEquals(2, section.getExercises().size());
            Exercise squat = section.getExercises().get(0);
            assertEquals("Back Squat", squat.getName());
            assertEquals(5, squat.getSets());
            assertEquals("5", squat.getReps());
            assertEquals("225 lbs", squat.getWeight());
            assertEquals(180, squat.getRestSeconds());
        }
    }

    // ========================================================================
    // AMRAP section parsing
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — AMRAP section")
    class AmrapSectionParsing {

        @Test
        void parseWorkout_ValidAmrapSection_ParsesTimeCap() throws ParsingException {
            Workout workout = WorkoutParser.parseWorkout(amrapWorkoutText(), GenerationScope.DAY);

            Section section = workout.getSections().get(0);
            assertEquals(SectionType.AMRAP, section.getType());
            assertEquals(12, section.getTimeCapMinutes());
            assertNull(section.getIntervalSeconds());
            assertNull(section.getTotalRounds());
            assertNull(section.getWorkIntervalSeconds());
            assertNull(section.getRestIntervalSeconds());

            // AMRAP exercises should have no rest seconds
            for (Exercise ex : section.getExercises()) {
                assertNull(ex.getRestSeconds(), "AMRAP exercises should not have restSeconds");
            }
        }

        @Test
        void parseWorkout_AmrapExerciseWithWeight_PreservesWeight() throws ParsingException {
            Workout workout = WorkoutParser.parseWorkout(amrapWorkoutText(), GenerationScope.DAY);

            Exercise boxJumps = workout.getSections().get(0).getExercises().get(1);
            assertEquals("Box Jumps", boxJumps.getName());
            assertEquals("bodyweight", boxJumps.getWeight());
        }
    }

    // ========================================================================
    // EMOM section parsing
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — EMOM section")
    class EmomSectionParsing {

        @Test
        void parseWorkout_ValidEmomSection_ParsesIntervalAndRounds() throws ParsingException {
            Workout workout = WorkoutParser.parseWorkout(emomWorkoutText(), GenerationScope.DAY);

            Section section = workout.getSections().get(0);
            assertEquals(SectionType.EMOM, section.getType());
            assertEquals(60, section.getIntervalSeconds());
            assertEquals(10, section.getTotalRounds());
            assertNull(section.getTimeCapMinutes());
            assertNull(section.getWorkIntervalSeconds());
            assertNull(section.getRestIntervalSeconds());

            for (Exercise ex : section.getExercises()) {
                assertNull(ex.getRestSeconds(), "EMOM exercises should not have restSeconds");
            }
        }
    }

    // ========================================================================
    // TABATA section parsing
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — TABATA section")
    class TabataSectionParsing {

        @Test
        void parseWorkout_ValidTabataSection_ParsesAllTimingFields() throws ParsingException {
            Workout workout = WorkoutParser.parseWorkout(tabataWorkoutText(), GenerationScope.DAY);

            Section section = workout.getSections().get(0);
            assertEquals(SectionType.TABATA, section.getType());
            assertEquals(20, section.getWorkIntervalSeconds());
            assertEquals(10, section.getRestIntervalSeconds());
            assertEquals(8, section.getTotalRounds());
            assertNull(section.getTimeCapMinutes());
            assertNull(section.getIntervalSeconds());

            assertEquals(2, section.getExercises().size());
            for (Exercise ex : section.getExercises()) {
                assertNull(ex.getRestSeconds(), "TABATA exercises should not have restSeconds");
            }
        }

        @Test
        void parseWorkout_TabataExerciseWithMaxReps_ParsesRepsAsString() throws ParsingException {
            Workout workout = WorkoutParser.parseWorkout(tabataWorkoutText(), GenerationScope.DAY);

            Exercise airSquats = workout.getSections().get(0).getExercises().get(0);
            assertEquals("max", airSquats.getReps());
        }
    }

    // ========================================================================
    // FOR_TIME section parsing
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — FOR_TIME section")
    class ForTimeSectionParsing {

        @Test
        void parseWorkout_ValidForTimeSection_ParsesTimeCap() throws ParsingException {
            Workout workout = WorkoutParser.parseWorkout(forTimeWorkoutText(), GenerationScope.DAY);

            Section section = workout.getSections().get(0);
            assertEquals(SectionType.FOR_TIME, section.getType());
            assertEquals(20, section.getTimeCapMinutes());
            assertNull(section.getIntervalSeconds());
            assertNull(section.getTotalRounds());

            for (Exercise ex : section.getExercises()) {
                assertNull(ex.getRestSeconds(), "FOR_TIME exercises should not have restSeconds");
            }
        }

        @Test
        void parseWorkout_ForTimeExerciseWithoutWeight_WeightIsNull() throws ParsingException {
            Workout workout = WorkoutParser.parseWorkout(forTimeWorkoutText(), GenerationScope.DAY);

            Exercise pullUps = workout.getSections().get(0).getExercises().get(1);
            assertEquals("Pull Ups", pullUps.getName());
            assertNull(pullUps.getWeight());
        }
    }

    // ========================================================================
    // ACCESSORY section parsing
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — ACCESSORY section")
    class AccessorySectionParsing {

        @Test
        void parseWorkout_ValidAccessorySection_ParsesRestPerExercise() throws ParsingException {
            Workout workout = WorkoutParser.parseWorkout(accessoryWorkoutText(), GenerationScope.DAY);

            Section section = workout.getSections().get(0);
            assertEquals(SectionType.ACCESSORY, section.getType());
            assertNull(section.getTimeCapMinutes());
            assertNull(section.getIntervalSeconds());
            assertNull(section.getTotalRounds());

            assertEquals(2, section.getExercises().size());
            for (Exercise ex : section.getExercises()) {
                assertNotNull(ex.getRestSeconds(), "ACCESSORY exercises must have restSeconds");
                assertEquals(60, ex.getRestSeconds());
            }
        }
    }

    // ========================================================================
    // Multi-section workout parsing
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — multi-section")
    class MultiSectionParsing {

        @Test
        void parseWorkout_MultipleSections_ParsesBothSections() throws ParsingException {
            Workout workout = WorkoutParser.parseWorkout(multiSectionWorkoutText(), GenerationScope.DAY);

            assertEquals(2, workout.getSections().size());

            Section strength = workout.getSections().get(0);
            assertEquals(SectionType.STRENGTH, strength.getType());
            assertEquals("Strength Block", strength.getName());

            Section amrap = workout.getSections().get(1);
            assertEquals(SectionType.AMRAP, amrap.getType());
            assertEquals("Conditioning", amrap.getName());
            assertEquals(10, amrap.getTimeCapMinutes());
        }
    }

    // ========================================================================
    // Program parsing
    // ========================================================================

    @Nested
    @DisplayName("parseProgram — valid programs")
    class ProgramParsing {

        @Test
        void parseProgram_ValidWeekProgram_ParsesAllDays() throws ParsingException {
            Program program = WorkoutParser.parseProgram(simpleProgramText(), GenerationScope.WEEK);

            assertEquals("Weekly Plan", program.getName());
            assertEquals("A simple weekly program", program.getDescription());
            assertEquals(GenerationScope.WEEK, program.getScope());
            assertEquals(2, program.getTrainingStyles().size());
            assertTrue(program.getTrainingStyles().contains(TrainingStyle.STRENGTH));
            assertTrue(program.getTrainingStyles().contains(TrainingStyle.CROSSFIT));
            assertEquals(2, program.getWorkouts().size());

            Workout day1 = program.getWorkouts().get(0);
            assertEquals("Day 1 Workout", day1.getName());
            assertEquals(TrainingStyle.STRENGTH, day1.getTrainingStyle());

            Workout day2 = program.getWorkouts().get(1);
            assertEquals("Day 2 Workout", day2.getName());
            assertEquals(TrainingStyle.CROSSFIT, day2.getTrainingStyle());
        }

        @Test
        void parseProgram_WeekScope_RejectsDayScope() {
            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseProgram(simpleProgramText(), GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("WEEK or FOUR_WEEK"));
        }
    }

    // ========================================================================
    // Scope validation
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — scope validation")
    class ScopeValidation {

        @Test
        void parseWorkout_WeekScope_ThrowsParsingException() {
            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(strengthWorkoutText(), GenerationScope.WEEK));
            assertTrue(ex.getMessage().contains("DAY scope"));
        }

        @Test
        void parseWorkout_FourWeekScope_ThrowsParsingException() {
            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(strengthWorkoutText(), GenerationScope.FOUR_WEEK));
            assertTrue(ex.getMessage().contains("DAY scope"));
        }
    }

    // ========================================================================
    // Malformed input
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — malformed input")
    class MalformedInput {

        @Test
        void parseWorkout_NullInput_ThrowsParsingException() {
            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(null, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("null or blank"));
        }

        @Test
        void parseWorkout_EmptyString_ThrowsParsingException() {
            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout("", GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("null or blank"));
        }

        @Test
        void parseWorkout_BlankString_ThrowsParsingException() {
            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout("   \n  \n  ", GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("null or blank"));
        }

        @Test
        void parseWorkout_MissingWorkoutPrefix_ThrowsParsingException() {
            String text = """
                    NOT_A_WORKOUT: Bad
                    DESCRIPTION: Something
                    TRAINING_STYLE: STRENGTH
                    --- SECTION: Block [TYPE: STRENGTH] ---
                    - Squat | Sets: 3 | Reps: 5 | Weight: 200 lbs | Rest: 120s""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("workout name"));
        }

        @Test
        void parseWorkout_MissingDescription_ThrowsParsingException() {
            String text = """
                    WORKOUT: Test
                    TRAINING_STYLE: STRENGTH
                    --- SECTION: Block [TYPE: STRENGTH] ---
                    - Squat | Sets: 3 | Reps: 5 | Weight: 200 lbs | Rest: 120s""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("workout description"));
        }

        @Test
        void parseWorkout_MissingTrainingStyle_ThrowsParsingException() {
            String text = """
                    WORKOUT: Test
                    DESCRIPTION: A test
                    --- SECTION: Block [TYPE: STRENGTH] ---
                    - Squat | Sets: 3 | Reps: 5 | Weight: 200 lbs | Rest: 120s""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("workout training style"));
        }

        @Test
        void parseWorkout_InvalidTrainingStyle_ThrowsParsingException() {
            String text = """
                    WORKOUT: Test
                    DESCRIPTION: A test
                    TRAINING_STYLE: INVALID_STYLE
                    --- SECTION: Block [TYPE: STRENGTH] ---
                    - Squat | Sets: 3 | Reps: 5 | Weight: 200 lbs | Rest: 120s""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("Invalid training style"));
        }

        @Test
        void parseWorkout_InvalidSectionType_ThrowsParsingException() {
            String text = """
                    WORKOUT: Test
                    DESCRIPTION: A test
                    TRAINING_STYLE: STRENGTH
                    --- SECTION: Block [TYPE: UNKNOWN] ---
                    - Squat | Sets: 3 | Reps: 5 | Weight: 200 lbs | Rest: 120s""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("Invalid section type"));
        }
    }

    // ========================================================================
    // Missing sections
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — missing sections")
    class MissingSections {

        @Test
        void parseWorkout_NoSections_ThrowsParsingException() {
            String text = """
                    WORKOUT: Empty Workout
                    DESCRIPTION: No sections here
                    TRAINING_STYLE: STRENGTH""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("at least one section"));
        }

        @Test
        void parseWorkout_SectionWithNoExercises_ThrowsParsingException() {
            String text = """
                    WORKOUT: Bad Workout
                    DESCRIPTION: Section has no exercises
                    TRAINING_STYLE: STRENGTH
                    --- SECTION: Empty Block [TYPE: STRENGTH] ---""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("at least one exercise"));
        }
    }

    // ========================================================================
    // Malformed exercise lines
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — malformed exercises")
    class MalformedExercises {

        @Test
        void parseWorkout_ExerciseMissingSetsField_ThrowsParsingException() {
            String text = """
                    WORKOUT: Bad Exercise
                    DESCRIPTION: Missing sets
                    TRAINING_STYLE: STRENGTH
                    --- SECTION: Block [TYPE: STRENGTH] ---
                    - Squat | Reps: 5 | Weight: 200 lbs | Rest: 120s""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("Invalid exercise format")
                    || ex.getMessage().contains("exercise"));
        }

        @Test
        void parseWorkout_UnexpectedLineInSection_ThrowsParsingException() {
            String text = """
                    WORKOUT: Bad Format
                    DESCRIPTION: Random line in section
                    TRAINING_STYLE: STRENGTH
                    --- SECTION: Block [TYPE: STRENGTH] ---
                    This is not an exercise line""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("Expected exercise line")
                    || ex.getMessage().contains("exercise"));
        }

        @Test
        void parseWorkout_GarbageText_ThrowsParsingException() {
            String text = "Just some random text that is not a workout format at all.";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertNotNull(ex.getMessage());
            assertFalse(ex.getMessage().isBlank());
        }
    }

    // ========================================================================
    // Missing timing fields for timed sections
    // ========================================================================

    @Nested
    @DisplayName("parseWorkout — missing timing fields")
    class MissingTimingFields {

        @Test
        void parseWorkout_AmrapMissingTimeCap_ThrowsParsingException() {
            String text = """
                    WORKOUT: Bad AMRAP
                    DESCRIPTION: Missing time cap
                    TRAINING_STYLE: CROSSFIT
                    --- SECTION: AMRAP Block [TYPE: AMRAP] ---
                    - Burpees | Sets: 1 | Reps: 10""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("time cap minutes")
                    || ex.getMessage().contains("TIME_CAP_MINUTES"));
        }

        @Test
        void parseWorkout_EmomMissingInterval_ThrowsParsingException() {
            String text = """
                    WORKOUT: Bad EMOM
                    DESCRIPTION: Missing interval
                    TRAINING_STYLE: CROSSFIT
                    --- SECTION: EMOM Block [TYPE: EMOM] ---
                    TOTAL_ROUNDS: 10
                    - Clean | Sets: 1 | Reps: 3""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("interval seconds")
                    || ex.getMessage().contains("INTERVAL_SECONDS"));
        }

        @Test
        void parseWorkout_TabataMissingWorkInterval_ThrowsParsingException() {
            String text = """
                    WORKOUT: Bad Tabata
                    DESCRIPTION: Missing work interval
                    TRAINING_STYLE: CROSSFIT
                    --- SECTION: Tabata Block [TYPE: TABATA] ---
                    REST_INTERVAL_SECONDS: 10
                    TOTAL_ROUNDS: 8
                    - Squats | Sets: 1 | Reps: max""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseWorkout(text, GenerationScope.DAY));
            assertTrue(ex.getMessage().contains("work interval seconds")
                    || ex.getMessage().contains("WORK_INTERVAL_SECONDS"));
        }
    }

    // ========================================================================
    // Program malformed input
    // ========================================================================

    @Nested
    @DisplayName("parseProgram — malformed input")
    class ProgramMalformedInput {

        @Test
        void parseProgram_NullInput_ThrowsParsingException() {
            assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseProgram(null, GenerationScope.WEEK));
        }

        @Test
        void parseProgram_EmptyInput_ThrowsParsingException() {
            assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseProgram("", GenerationScope.WEEK));
        }

        @Test
        void parseProgram_ScopeMismatch_ThrowsParsingException() {
            // Text declares WEEK but we pass FOUR_WEEK
            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseProgram(simpleProgramText(), GenerationScope.FOUR_WEEK));
            assertTrue(ex.getMessage().contains("Scope mismatch"));
        }

        @Test
        void parseProgram_MissingDayHeaders_ThrowsParsingException() {
            String text = """
                    PROGRAM: Bad Program
                    DESCRIPTION: No day headers
                    SCOPE: WEEK
                    TRAINING_STYLES: STRENGTH

                    WORKOUT: Orphan Workout
                    DESCRIPTION: Not under a day header
                    TRAINING_STYLE: STRENGTH
                    --- SECTION: Block [TYPE: STRENGTH] ---
                    - Squat | Sets: 3 | Reps: 5 | Weight: 200 lbs | Rest: 120s""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseProgram(text, GenerationScope.WEEK));
            assertTrue(ex.getMessage().contains("DAY header"));
        }

        @Test
        void parseProgram_NoWorkoutsAfterHeader_ThrowsParsingException() {
            String text = """
                    PROGRAM: Empty Program
                    DESCRIPTION: No workouts
                    SCOPE: WEEK
                    TRAINING_STYLES: STRENGTH""";

            ParsingException ex = assertThrows(ParsingException.class,
                    () -> WorkoutParser.parseProgram(text, GenerationScope.WEEK));
            assertTrue(ex.getMessage().contains("at least one workout"));
        }
    }
}
