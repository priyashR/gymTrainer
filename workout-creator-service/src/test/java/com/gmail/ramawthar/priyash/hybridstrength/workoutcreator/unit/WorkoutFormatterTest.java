package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WorkoutFormatter}.
 * <p>
 * Tests formatting examples for each SectionType and edge cases
 * (missing optional fields like null weight and null restSeconds).
 * <p>
 * Naming convention: MethodName_StateUnderTest_ExpectedBehaviour
 * No Spring context — plain Java only.
 *
 * @see WorkoutFormatter
 * Requirements: 3.1, 3.3
 */
class WorkoutFormatterTest {

    // ---- Helper methods for building domain objects ----

    private static Exercise strengthExercise(String name, int sets, String reps, String weight, int restSeconds) {
        return new Exercise(name, sets, reps, weight, restSeconds);
    }

    private static Exercise timedExercise(String name, int sets, String reps, String weight) {
        return new Exercise(name, sets, reps, weight, null);
    }

    private static Exercise timedExerciseNoWeight(String name, int sets, String reps) {
        return new Exercise(name, sets, reps, null, null);
    }

    private static Section strengthSection(String name, List<Exercise> exercises) {
        return new Section(name, SectionType.STRENGTH, exercises, null, null, null, null, null);
    }

    private static Section amrapSection(String name, int timeCapMinutes, List<Exercise> exercises) {
        return new Section(name, SectionType.AMRAP, exercises, timeCapMinutes, null, null, null, null);
    }

    private static Section emomSection(String name, int intervalSeconds, int totalRounds, List<Exercise> exercises) {
        return new Section(name, SectionType.EMOM, exercises, null, intervalSeconds, totalRounds, null, null);
    }

    private static Section tabataSection(String name, int workSeconds, int restSeconds, int totalRounds, List<Exercise> exercises) {
        return new Section(name, SectionType.TABATA, exercises, null, null, totalRounds, workSeconds, restSeconds);
    }

    private static Section forTimeSection(String name, int timeCapMinutes, List<Exercise> exercises) {
        return new Section(name, SectionType.FOR_TIME, exercises, timeCapMinutes, null, null, null, null);
    }

    private static Section accessorySection(String name, List<Exercise> exercises) {
        return new Section(name, SectionType.ACCESSORY, exercises, null, null, null, null, null);
    }

    private static Workout workout(String name, String description, TrainingStyle style, List<Section> sections) {
        return new Workout(name, description, style, sections);
    }

    // ========================================================================
    // Null input handling
    // ========================================================================

    @Nested
    @DisplayName("format — null input")
    class NullInput {

        @Test
        void format_NullWorkout_ThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> WorkoutFormatter.format((Workout) null));
        }

        @Test
        void format_NullProgram_ThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> WorkoutFormatter.format((Program) null));
        }
    }

    // ========================================================================
    // STRENGTH section formatting
    // ========================================================================

    @Nested
    @DisplayName("format — STRENGTH section")
    class StrengthSectionFormatting {

        @Test
        void format_StrengthSection_IncludesRestPerExercise() {
            Workout w = workout("Strength Day", "Heavy lifts", TrainingStyle.STRENGTH, List.of(
                    strengthSection("Main Lifts", List.of(
                            strengthExercise("Back Squat", 5, "5", "225 lbs", 180),
                            strengthExercise("Bench Press", 4, "6", "185 lbs", 120)
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("WORKOUT: Strength Day"));
            assertTrue(result.contains("DESCRIPTION: Heavy lifts"));
            assertTrue(result.contains("TRAINING_STYLE: STRENGTH"));
            assertTrue(result.contains("--- SECTION: Main Lifts [TYPE: STRENGTH] ---"));
            assertTrue(result.contains("- Back Squat | Sets: 5 | Reps: 5 | Weight: 225 lbs | Rest: 180s"));
            assertTrue(result.contains("- Bench Press | Sets: 4 | Reps: 6 | Weight: 185 lbs | Rest: 120s"));
            // STRENGTH sections should not have section-level timing
            assertFalse(result.contains("TIME_CAP_MINUTES"));
            assertFalse(result.contains("INTERVAL_SECONDS"));
            assertFalse(result.contains("TOTAL_ROUNDS"));
        }

        @Test
        void format_StrengthExerciseWithNullWeight_OmitsWeightField() {
            Workout w = workout("Bodyweight Strength", "No equipment", TrainingStyle.STRENGTH, List.of(
                    strengthSection("Calisthenics", List.of(
                            strengthExercise("Pull Ups", 4, "8", null, 90)
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("- Pull Ups | Sets: 4 | Reps: 8 | Rest: 90s"));
            assertFalse(result.contains("Weight:"));
        }
    }

    // ========================================================================
    // AMRAP section formatting
    // ========================================================================

    @Nested
    @DisplayName("format — AMRAP section")
    class AmrapSectionFormatting {

        @Test
        void format_AmrapSection_IncludesTimeCapAndNoPerExerciseRest() {
            Workout w = workout("AMRAP Burner", "High intensity", TrainingStyle.CROSSFIT, List.of(
                    amrapSection("AMRAP Block", 12, List.of(
                            timedExercise("Burpees", 1, "10", "bodyweight"),
                            timedExerciseNoWeight("Box Jumps", 1, "15")
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("--- SECTION: AMRAP Block [TYPE: AMRAP] ---"));
            assertTrue(result.contains("TIME_CAP_MINUTES: 12"));
            assertTrue(result.contains("- Burpees | Sets: 1 | Reps: 10 | Weight: bodyweight"));
            assertTrue(result.contains("- Box Jumps | Sets: 1 | Reps: 15"));
            assertFalse(result.contains("Rest:"));
        }
    }

    // ========================================================================
    // EMOM section formatting
    // ========================================================================

    @Nested
    @DisplayName("format — EMOM section")
    class EmomSectionFormatting {

        @Test
        void format_EmomSection_IncludesIntervalAndRounds() {
            Workout w = workout("EMOM Session", "Every minute on the minute", TrainingStyle.CROSSFIT, List.of(
                    emomSection("EMOM Block", 60, 10, List.of(
                            timedExercise("Power Clean", 1, "3", "135 lbs")
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("--- SECTION: EMOM Block [TYPE: EMOM] ---"));
            assertTrue(result.contains("INTERVAL_SECONDS: 60"));
            assertTrue(result.contains("TOTAL_ROUNDS: 10"));
            assertTrue(result.contains("- Power Clean | Sets: 1 | Reps: 3 | Weight: 135 lbs"));
            assertFalse(result.contains("TIME_CAP_MINUTES"));
            assertFalse(result.contains("Rest:"));
        }
    }

    // ========================================================================
    // TABATA section formatting
    // ========================================================================

    @Nested
    @DisplayName("format — TABATA section")
    class TabataSectionFormatting {

        @Test
        void format_TabataSection_IncludesWorkRestAndRounds() {
            Workout w = workout("Tabata Blast", "Tabata intervals", TrainingStyle.CROSSFIT, List.of(
                    tabataSection("Tabata Round", 20, 10, 8, List.of(
                            timedExerciseNoWeight("Air Squats", 1, "max"),
                            timedExerciseNoWeight("Push Ups", 1, "max")
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("--- SECTION: Tabata Round [TYPE: TABATA] ---"));
            assertTrue(result.contains("WORK_INTERVAL_SECONDS: 20"));
            assertTrue(result.contains("REST_INTERVAL_SECONDS: 10"));
            assertTrue(result.contains("TOTAL_ROUNDS: 8"));
            assertTrue(result.contains("- Air Squats | Sets: 1 | Reps: max"));
            assertTrue(result.contains("- Push Ups | Sets: 1 | Reps: max"));
            assertFalse(result.contains("TIME_CAP_MINUTES"));
            // EMOM uses INTERVAL_SECONDS (without WORK_/REST_ prefix) — Tabata should not have it
            assertFalse(result.contains("\nINTERVAL_SECONDS:"));
        }
    }

    // ========================================================================
    // FOR_TIME section formatting
    // ========================================================================

    @Nested
    @DisplayName("format — FOR_TIME section")
    class ForTimeSectionFormatting {

        @Test
        void format_ForTimeSection_IncludesTimeCap() {
            Workout w = workout("For Time WOD", "Complete ASAP", TrainingStyle.CROSSFIT, List.of(
                    forTimeSection("The Workout", 20, List.of(
                            timedExercise("Thrusters", 1, "21", "95 lbs"),
                            timedExerciseNoWeight("Pull Ups", 1, "21")
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("--- SECTION: The Workout [TYPE: FOR_TIME] ---"));
            assertTrue(result.contains("TIME_CAP_MINUTES: 20"));
            assertTrue(result.contains("- Thrusters | Sets: 1 | Reps: 21 | Weight: 95 lbs"));
            assertTrue(result.contains("- Pull Ups | Sets: 1 | Reps: 21"));
            assertFalse(result.contains("Rest:"));
        }
    }

    // ========================================================================
    // ACCESSORY section formatting
    // ========================================================================

    @Nested
    @DisplayName("format — ACCESSORY section")
    class AccessorySectionFormatting {

        @Test
        void format_AccessorySection_IncludesRestPerExercise() {
            Workout w = workout("Accessory Work", "Isolation movements", TrainingStyle.HYPERTROPHY, List.of(
                    accessorySection("Accessories", List.of(
                            strengthExercise("Bicep Curls", 3, "12", "30 lbs", 60),
                            strengthExercise("Tricep Pushdowns", 3, "15", "40 lbs", 60)
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("--- SECTION: Accessories [TYPE: ACCESSORY] ---"));
            assertTrue(result.contains("- Bicep Curls | Sets: 3 | Reps: 12 | Weight: 30 lbs | Rest: 60s"));
            assertTrue(result.contains("- Tricep Pushdowns | Sets: 3 | Reps: 15 | Weight: 40 lbs | Rest: 60s"));
            assertFalse(result.contains("TIME_CAP_MINUTES"));
            assertFalse(result.contains("INTERVAL_SECONDS"));
        }

        @Test
        void format_AccessoryExerciseWithNullWeight_OmitsWeightField() {
            Workout w = workout("Bodyweight Accessories", "No equipment", TrainingStyle.HYPERTROPHY, List.of(
                    accessorySection("Bodyweight", List.of(
                            strengthExercise("Dips", 3, "10", null, 60)
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("- Dips | Sets: 3 | Reps: 10 | Rest: 60s"));
            assertFalse(result.contains("Weight:"));
        }
    }


    // ========================================================================
    // Multi-section formatting
    // ========================================================================

    @Nested
    @DisplayName("format — multi-section workout")
    class MultiSectionFormatting {

        @Test
        void format_MultipleSections_SeparatedByBlankLine() {
            Workout w = workout("Full Session", "Strength plus conditioning", TrainingStyle.CROSSFIT, List.of(
                    strengthSection("Strength Block", List.of(
                            strengthExercise("Deadlift", 5, "3", "315 lbs", 180)
                    )),
                    amrapSection("Conditioning", 10, List.of(
                            timedExercise("Wall Balls", 1, "20", "20 lbs"),
                            timedExerciseNoWeight("Rowing", 1, "250")
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            // Sections should be separated by a blank line
            assertTrue(result.contains("Rest: 180s\n\n--- SECTION: Conditioning"));
            assertTrue(result.contains("--- SECTION: Strength Block [TYPE: STRENGTH] ---"));
            assertTrue(result.contains("--- SECTION: Conditioning [TYPE: AMRAP] ---"));
            assertTrue(result.contains("TIME_CAP_MINUTES: 10"));
        }

        @Test
        void format_ThreeSections_AllPresent() {
            Workout w = workout("Triple Threat", "Three section workout", TrainingStyle.CROSSFIT, List.of(
                    strengthSection("Warm Up", List.of(
                            strengthExercise("Barbell Row", 3, "10", "135 lbs", 90)
                    )),
                    emomSection("EMOM Work", 60, 10, List.of(
                            timedExercise("Snatch", 1, "2", "95 lbs")
                    )),
                    forTimeSection("Finisher", 15, List.of(
                            timedExerciseNoWeight("Burpees", 1, "50")
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("[TYPE: STRENGTH]"));
            assertTrue(result.contains("[TYPE: EMOM]"));
            assertTrue(result.contains("[TYPE: FOR_TIME]"));
            assertTrue(result.contains("INTERVAL_SECONDS: 60"));
            assertTrue(result.contains("TOTAL_ROUNDS: 10"));
            assertTrue(result.contains("TIME_CAP_MINUTES: 15"));
        }
    }

    // ========================================================================
    // Program formatting
    // ========================================================================

    @Nested
    @DisplayName("format — Program")
    class ProgramFormatting {

        @Test
        void format_Program_IncludesHeaderAndDayMarkers() {
            Workout day1 = workout("Day 1 Workout", "Strength focus", TrainingStyle.STRENGTH, List.of(
                    strengthSection("Main Lifts", List.of(
                            strengthExercise("Squat", 5, "5", "200 lbs", 180)
                    ))
            ));
            Workout day2 = workout("Day 2 Workout", "Conditioning focus", TrainingStyle.CROSSFIT, List.of(
                    amrapSection("WOD", 15, List.of(
                            timedExerciseNoWeight("Burpees", 1, "10")
                    ))
            ));

            Program program = new Program("Weekly Plan", "A simple weekly program",
                    GenerationScope.WEEK, List.of(TrainingStyle.STRENGTH, TrainingStyle.CROSSFIT),
                    List.of(day1, day2));

            String result = WorkoutFormatter.format(program);

            assertTrue(result.contains("PROGRAM: Weekly Plan"));
            assertTrue(result.contains("DESCRIPTION: A simple weekly program"));
            assertTrue(result.contains("SCOPE: WEEK"));
            assertTrue(result.contains("TRAINING_STYLES: STRENGTH, CROSSFIT"));
            assertTrue(result.contains("=== DAY 1 ==="));
            assertTrue(result.contains("=== DAY 2 ==="));
            assertTrue(result.contains("WORKOUT: Day 1 Workout"));
            assertTrue(result.contains("WORKOUT: Day 2 Workout"));
        }

        @Test
        void format_FourWeekProgram_UsesCorrectScope() {
            Workout day1 = workout("Day 1", "First day", TrainingStyle.HYPERTROPHY, List.of(
                    accessorySection("Arms", List.of(
                            strengthExercise("Curls", 3, "12", "25 lbs", 60)
                    ))
            ));

            Program program = new Program("4-Week Hypertrophy", "Month-long program",
                    GenerationScope.FOUR_WEEK, List.of(TrainingStyle.HYPERTROPHY),
                    List.of(day1));

            String result = WorkoutFormatter.format(program);

            assertTrue(result.contains("SCOPE: FOUR_WEEK"));
            assertTrue(result.contains("TRAINING_STYLES: HYPERTROPHY"));
        }

        @Test
        void format_ProgramWithMultipleStyles_ListsAllStyles() {
            Workout day1 = workout("Day 1", "Strength", TrainingStyle.STRENGTH, List.of(
                    strengthSection("Lifts", List.of(
                            strengthExercise("Squat", 5, "5", "200 lbs", 180)
                    ))
            ));

            Program program = new Program("Mixed Program", "Multi-style",
                    GenerationScope.WEEK,
                    List.of(TrainingStyle.CROSSFIT, TrainingStyle.HYPERTROPHY, TrainingStyle.STRENGTH),
                    List.of(day1));

            String result = WorkoutFormatter.format(program);

            assertTrue(result.contains("TRAINING_STYLES: CROSSFIT, HYPERTROPHY, STRENGTH"));
        }
    }

    // ========================================================================
    // Edge cases — missing optional fields
    // ========================================================================

    @Nested
    @DisplayName("format — edge cases")
    class EdgeCases {

        @Test
        void format_ExerciseWithNullWeightAndNullRest_OmitsBothFields() {
            // Timed section exercises have null weight and null restSeconds
            Workout w = workout("Minimal AMRAP", "Bodyweight only", TrainingStyle.CROSSFIT, List.of(
                    amrapSection("Bodyweight AMRAP", 8, List.of(
                            timedExerciseNoWeight("Air Squats", 1, "20")
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("- Air Squats | Sets: 1 | Reps: 20"));
            assertFalse(result.contains("Weight:"));
            assertFalse(result.contains("Rest:"));
        }

        @Test
        void format_ExerciseWithWeightButNullRest_IncludesWeightOnly() {
            Workout w = workout("Weighted AMRAP", "With weights", TrainingStyle.CROSSFIT, List.of(
                    amrapSection("Weighted Block", 10, List.of(
                            timedExercise("Kettlebell Swings", 1, "15", "53 lbs")
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("- Kettlebell Swings | Sets: 1 | Reps: 15 | Weight: 53 lbs"));
            assertFalse(result.contains("Rest:"));
        }

        @Test
        void format_ExerciseWithRepRange_PreservesRepString() {
            Workout w = workout("Hypertrophy Day", "Rep ranges", TrainingStyle.HYPERTROPHY, List.of(
                    accessorySection("Volume Work", List.of(
                            strengthExercise("Lateral Raises", 4, "12-15", "15 lbs", 45)
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("Reps: 12-15"));
        }

        @Test
        void format_ExerciseWithMaxReps_PreservesMaxString() {
            Workout w = workout("Max Effort", "Go to failure", TrainingStyle.CROSSFIT, List.of(
                    tabataSection("Tabata Set", 20, 10, 8, List.of(
                            timedExerciseNoWeight("Push Ups", 1, "max")
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("Reps: max"));
        }

        @Test
        void format_ExerciseWithBodyweightString_PreservesWeightString() {
            Workout w = workout("BW Workout", "Bodyweight training", TrainingStyle.STRENGTH, List.of(
                    strengthSection("Main", List.of(
                            strengthExercise("Chin Ups", 4, "8", "bodyweight", 120)
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("Weight: bodyweight"));
        }

        @Test
        void format_OutputDoesNotHaveTrailingWhitespace() {
            Workout w = workout("Clean Output", "No trailing spaces", TrainingStyle.STRENGTH, List.of(
                    strengthSection("Block", List.of(
                            strengthExercise("Squat", 3, "5", "200 lbs", 120)
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertEquals(result, result.stripTrailing());
        }
    }

    // ========================================================================
    // Output completeness — all domain fields present (Requirement 3.3)
    // ========================================================================

    @Nested
    @DisplayName("format — output completeness")
    class OutputCompleteness {

        @Test
        void format_AllFieldsPresent_OutputContainsEveryDomainField() {
            Workout w = workout("Complete Workout", "All fields populated", TrainingStyle.STRENGTH, List.of(
                    strengthSection("Heavy Lifts", List.of(
                            strengthExercise("Back Squat", 5, "5", "225 lbs", 180),
                            strengthExercise("Overhead Press", 4, "8", "95 lbs", 120)
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            // Section name and type
            assertTrue(result.contains("Heavy Lifts"));
            assertTrue(result.contains("STRENGTH"));

            // All exercise fields
            assertTrue(result.contains("Back Squat"));
            assertTrue(result.contains("Sets: 5"));
            assertTrue(result.contains("Reps: 5"));
            assertTrue(result.contains("Weight: 225 lbs"));
            assertTrue(result.contains("Rest: 180s"));

            assertTrue(result.contains("Overhead Press"));
            assertTrue(result.contains("Sets: 4"));
            assertTrue(result.contains("Reps: 8"));
            assertTrue(result.contains("Weight: 95 lbs"));
            assertTrue(result.contains("Rest: 120s"));
        }

        @Test
        void format_EmomSection_OutputContainsAllTimingFields() {
            Workout w = workout("EMOM Complete", "All EMOM fields", TrainingStyle.CROSSFIT, List.of(
                    emomSection("EMOM Work", 45, 12, List.of(
                            timedExercise("Clean and Jerk", 1, "2", "155 lbs")
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("EMOM Work"));
            assertTrue(result.contains("EMOM"));
            assertTrue(result.contains("INTERVAL_SECONDS: 45"));
            assertTrue(result.contains("TOTAL_ROUNDS: 12"));
            assertTrue(result.contains("Clean and Jerk"));
            assertTrue(result.contains("Sets: 1"));
            assertTrue(result.contains("Reps: 2"));
            assertTrue(result.contains("Weight: 155 lbs"));
        }

        @Test
        void format_TabataSection_OutputContainsAllTimingFields() {
            Workout w = workout("Tabata Complete", "All Tabata fields", TrainingStyle.CROSSFIT, List.of(
                    tabataSection("Tabata Intervals", 20, 10, 8, List.of(
                            timedExerciseNoWeight("Mountain Climbers", 1, "max")
                    ))
            ));

            String result = WorkoutFormatter.format(w);

            assertTrue(result.contains("Tabata Intervals"));
            assertTrue(result.contains("TABATA"));
            assertTrue(result.contains("WORK_INTERVAL_SECONDS: 20"));
            assertTrue(result.contains("REST_INTERVAL_SECONDS: 10"));
            assertTrue(result.contains("TOTAL_ROUNDS: 8"));
            assertTrue(result.contains("Mountain Climbers"));
        }
    }
}
