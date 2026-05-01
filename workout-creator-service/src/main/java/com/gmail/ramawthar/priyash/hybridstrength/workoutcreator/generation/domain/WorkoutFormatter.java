package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

import java.util.List;

/**
 * Formats {@link Workout} or {@link Program} domain objects into a deterministic
 * human-readable text representation.
 * <p>
 * The output format is designed to be unambiguous so that
 * {@link WorkoutParser} can reconstruct the original domain object exactly,
 * satisfying the round-trip property: {@code parse(format(x)) ≡ x}.
 * <p>
 * Stateless utility — no framework imports.
 *
 * <h3>Format specification</h3>
 * <pre>
 * WORKOUT: {name}
 * DESCRIPTION: {description}
 * TRAINING_STYLE: {trainingStyle}
 * --- SECTION: {name} [TYPE: {sectionType}] ---
 * (timing line depending on section type)
 * - {exerciseName} | Sets: {sets} | Reps: {reps} | Weight: {weight} | Rest: {restSeconds}s
 * (blank line between sections)
 * </pre>
 *
 * For programs, each workout is preceded by {@code === DAY {n} ===} and separated
 * by a blank line. The program header uses {@code PROGRAM:}, {@code SCOPE:}, and
 * {@code TRAINING_STYLES:} prefixes.
 */
public final class WorkoutFormatter {

    static final String WORKOUT_PREFIX = "WORKOUT: ";
    static final String DESCRIPTION_PREFIX = "DESCRIPTION: ";
    static final String TRAINING_STYLE_PREFIX = "TRAINING_STYLE: ";
    static final String SECTION_START = "--- SECTION: ";
    static final String SECTION_TYPE_MARKER = " [TYPE: ";
    static final String SECTION_END = "] ---";
    static final String TIME_CAP_PREFIX = "TIME_CAP_MINUTES: ";
    static final String INTERVAL_PREFIX = "INTERVAL_SECONDS: ";
    static final String TOTAL_ROUNDS_PREFIX = "TOTAL_ROUNDS: ";
    static final String WORK_INTERVAL_PREFIX = "WORK_INTERVAL_SECONDS: ";
    static final String REST_INTERVAL_PREFIX = "REST_INTERVAL_SECONDS: ";
    static final String EXERCISE_PREFIX = "- ";
    static final String PROGRAM_PREFIX = "PROGRAM: ";
    static final String SCOPE_PREFIX = "SCOPE: ";
    static final String TRAINING_STYLES_PREFIX = "TRAINING_STYLES: ";
    static final String DAY_PREFIX = "=== DAY ";
    static final String DAY_SUFFIX = " ===";

    private WorkoutFormatter() {
        // Utility class — not instantiable
    }

    /**
     * Formats a {@link Workout} into human-readable text.
     *
     * @param workout the workout to format; must not be null
     * @return the formatted text representation
     */
    public static String format(Workout workout) {
        if (workout == null) {
            throw new IllegalArgumentException("Workout must not be null");
        }
        StringBuilder sb = new StringBuilder();
        appendWorkout(sb, workout);
        return sb.toString().stripTrailing();
    }

    /**
     * Formats a {@link Program} into human-readable text.
     *
     * @param program the program to format; must not be null
     * @return the formatted text representation
     */
    public static String format(Program program) {
        if (program == null) {
            throw new IllegalArgumentException("Program must not be null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(PROGRAM_PREFIX).append(program.getName()).append("\n");
        sb.append(DESCRIPTION_PREFIX).append(program.getDescription()).append("\n");
        sb.append(SCOPE_PREFIX).append(program.getScope().name()).append("\n");
        sb.append(TRAINING_STYLES_PREFIX)
                .append(formatTrainingStyles(program.getTrainingStyles()))
                .append("\n");

        List<Workout> workouts = program.getWorkouts();
        for (int i = 0; i < workouts.size(); i++) {
            sb.append("\n");
            sb.append(DAY_PREFIX).append(i + 1).append(DAY_SUFFIX).append("\n");
            appendWorkout(sb, workouts.get(i));
        }
        return sb.toString().stripTrailing();
    }

    private static void appendWorkout(StringBuilder sb, Workout workout) {
        sb.append(WORKOUT_PREFIX).append(workout.getName()).append("\n");
        sb.append(DESCRIPTION_PREFIX).append(workout.getDescription()).append("\n");
        sb.append(TRAINING_STYLE_PREFIX).append(workout.getTrainingStyle().name()).append("\n");

        List<Section> sections = workout.getSections();
        for (int i = 0; i < sections.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            appendSection(sb, sections.get(i));
        }
    }

    private static void appendSection(StringBuilder sb, Section section) {
        sb.append(SECTION_START).append(section.getName())
                .append(SECTION_TYPE_MARKER).append(section.getType().name())
                .append(SECTION_END).append("\n");

        appendTimingFields(sb, section);

        for (Exercise exercise : section.getExercises()) {
            appendExercise(sb, exercise);
        }
    }

    private static void appendTimingFields(StringBuilder sb, Section section) {
        switch (section.getType()) {
            case AMRAP, FOR_TIME ->
                    sb.append(TIME_CAP_PREFIX).append(section.getTimeCapMinutes()).append("\n");
            case EMOM -> {
                sb.append(INTERVAL_PREFIX).append(section.getIntervalSeconds()).append("\n");
                sb.append(TOTAL_ROUNDS_PREFIX).append(section.getTotalRounds()).append("\n");
            }
            case TABATA -> {
                sb.append(WORK_INTERVAL_PREFIX).append(section.getWorkIntervalSeconds()).append("\n");
                sb.append(REST_INTERVAL_PREFIX).append(section.getRestIntervalSeconds()).append("\n");
                sb.append(TOTAL_ROUNDS_PREFIX).append(section.getTotalRounds()).append("\n");
            }
            case STRENGTH, ACCESSORY -> {
                // No section-level timing; rest is per-exercise
            }
        }
    }

    private static void appendExercise(StringBuilder sb, Exercise exercise) {
        sb.append(EXERCISE_PREFIX).append(exercise.getName())
                .append(" | Sets: ").append(exercise.getSets())
                .append(" | Reps: ").append(exercise.getReps());

        if (exercise.getWeight() != null) {
            sb.append(" | Weight: ").append(exercise.getWeight());
        }
        if (exercise.getRestSeconds() != null) {
            sb.append(" | Rest: ").append(exercise.getRestSeconds()).append("s");
        }
        sb.append("\n");
    }

    private static String formatTrainingStyles(List<TrainingStyle> styles) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < styles.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(styles.get(i).name());
        }
        return sb.toString();
    }
}
