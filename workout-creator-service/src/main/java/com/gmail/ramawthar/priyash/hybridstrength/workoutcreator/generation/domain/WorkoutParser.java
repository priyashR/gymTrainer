package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses sanitised Gemini text into {@link Workout} (for DAY scope) or
 * {@link Program} (for WEEK/FOUR_WEEK scope) domain objects.
 * <p>
 * The parser expects text in the deterministic format produced by
 * {@link WorkoutFormatter}, satisfying the round-trip property:
 * {@code parse(format(x)) ≡ x}.
 * <p>
 * Throws a checked {@link ParsingException} with a human-readable message
 * on any parsing failure.
 * <p>
 * Stateless utility — no framework imports.
 */
public final class WorkoutParser {

    private static final Pattern SECTION_HEADER_PATTERN =
            Pattern.compile("^--- SECTION:\\s*(.+?)\\s*\\[TYPE:\\s*(\\w+)]\\s*---$");

    private static final Pattern EXERCISE_PATTERN =
            Pattern.compile("^-\\s+(.+?)\\s*\\|\\s*Sets:\\s*(\\d+)\\s*\\|\\s*Reps:\\s*(.+?)(?:\\s*\\|\\s*Weight:\\s*(.+?))?(?:\\s*\\|\\s*Rest:\\s*(\\d+)s)?\\s*$");

    private static final Pattern DAY_HEADER_PATTERN =
            Pattern.compile("^===\\s*DAY\\s+(\\d+)\\s*===$");

    private WorkoutParser() {
        // Utility class — not instantiable
    }

    /**
     * Parses sanitised text into a {@link Workout} (for DAY scope).
     *
     * @param text  the sanitised Gemini text; must not be null or blank
     * @param scope the generation scope; must be {@link GenerationScope#DAY}
     * @return the parsed Workout
     * @throws ParsingException if the text cannot be parsed into a valid Workout
     */
    public static Workout parseWorkout(String text, GenerationScope scope) throws ParsingException {
        validateInput(text);
        if (scope != GenerationScope.DAY) {
            throw new ParsingException("parseWorkout requires DAY scope, got: " + scope);
        }
        List<String> lines = toLines(text);
        int[] cursor = {0};
        return parseWorkoutBlock(lines, cursor);
    }

    /**
     * Parses sanitised text into a {@link Program} (for WEEK/FOUR_WEEK scope).
     *
     * @param text  the sanitised Gemini text; must not be null or blank
     * @param scope the generation scope; must be WEEK or FOUR_WEEK
     * @return the parsed Program
     * @throws ParsingException if the text cannot be parsed into a valid Program
     */
    public static Program parseProgram(String text, GenerationScope scope) throws ParsingException {
        validateInput(text);
        if (scope == GenerationScope.DAY) {
            throw new ParsingException("parseProgram requires WEEK or FOUR_WEEK scope, got: DAY");
        }
        List<String> lines = toLines(text);
        int[] cursor = {0};
        return parseProgramBlock(lines, cursor, scope);
    }

    private static void validateInput(String text) throws ParsingException {
        if (text == null || text.isBlank()) {
            throw new ParsingException("Input text must not be null or blank");
        }
    }

    private static List<String> toLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            lines.add(line);
        }
        // Remove trailing empty lines
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    // ---- Program parsing ----

    private static Program parseProgramBlock(List<String> lines, int[] cursor,
                                             GenerationScope scope) throws ParsingException {
        String name = expectPrefix(lines, cursor, "PROGRAM: ", "program name");
        String description = expectPrefix(lines, cursor, "DESCRIPTION: ", "program description");
        String scopeStr = expectPrefix(lines, cursor, "SCOPE: ", "program scope");
        String stylesStr = expectPrefix(lines, cursor, "TRAINING_STYLES: ", "program training styles");

        GenerationScope parsedScope = parseScope(scopeStr);
        if (parsedScope != scope) {
            throw new ParsingException("Scope mismatch: expected " + scope + " but text declares " + parsedScope);
        }

        List<TrainingStyle> trainingStyles = parseTrainingStyles(stylesStr);
        List<Workout> workouts = new ArrayList<>();

        // Skip blank lines before first DAY header
        skipBlankLines(lines, cursor);

        while (cursor[0] < lines.size()) {
            skipBlankLines(lines, cursor);
            if (cursor[0] >= lines.size()) break;

            String line = lines.get(cursor[0]);
            Matcher dayMatcher = DAY_HEADER_PATTERN.matcher(line);
            if (!dayMatcher.matches()) {
                throw new ParsingException("Expected DAY header at line " + (cursor[0] + 1)
                        + ", got: " + line);
            }
            cursor[0]++;
            Workout workout = parseWorkoutBlock(lines, cursor);
            workouts.add(workout);
        }

        if (workouts.isEmpty()) {
            throw new ParsingException("Program must contain at least one workout");
        }

        try {
            return new Program(name, description, scope, trainingStyles, workouts);
        } catch (IllegalArgumentException e) {
            throw new ParsingException("Invalid program: " + e.getMessage(), e);
        }
    }

    // ---- Workout parsing ----

    private static Workout parseWorkoutBlock(List<String> lines, int[] cursor) throws ParsingException {
        String name = expectPrefix(lines, cursor, "WORKOUT: ", "workout name");
        String description = expectPrefix(lines, cursor, "DESCRIPTION: ", "workout description");
        String styleStr = expectPrefix(lines, cursor, "TRAINING_STYLE: ", "workout training style");

        TrainingStyle trainingStyle = parseTrainingStyle(styleStr);
        List<Section> sections = new ArrayList<>();

        while (cursor[0] < lines.size()) {
            skipBlankLines(lines, cursor);
            if (cursor[0] >= lines.size()) break;

            String line = lines.get(cursor[0]);
            // Stop if we hit a DAY header (next workout in a program)
            if (DAY_HEADER_PATTERN.matcher(line).matches()) break;
            // Stop if we hit a PROGRAM header (shouldn't happen in normal flow)
            if (line.startsWith("PROGRAM: ")) break;

            if (SECTION_HEADER_PATTERN.matcher(line).matches()) {
                sections.add(parseSectionBlock(lines, cursor));
            } else {
                throw new ParsingException("Expected section header at line " + (cursor[0] + 1)
                        + ", got: " + line);
            }
        }

        if (sections.isEmpty()) {
            throw new ParsingException("Workout '" + name + "' must contain at least one section");
        }

        try {
            return new Workout(name, description, trainingStyle, sections);
        } catch (IllegalArgumentException e) {
            throw new ParsingException("Invalid workout '" + name + "': " + e.getMessage(), e);
        }
    }

    // ---- Section parsing ----

    private static Section parseSectionBlock(List<String> lines, int[] cursor) throws ParsingException {
        String headerLine = lines.get(cursor[0]);
        Matcher headerMatcher = SECTION_HEADER_PATTERN.matcher(headerLine);
        if (!headerMatcher.matches()) {
            throw new ParsingException("Invalid section header at line " + (cursor[0] + 1)
                    + ": " + headerLine);
        }
        String sectionName = headerMatcher.group(1);
        SectionType sectionType = parseSectionType(headerMatcher.group(2));
        cursor[0]++;

        // Parse timing fields based on section type
        Integer timeCapMinutes = null;
        Integer intervalSeconds = null;
        Integer totalRounds = null;
        Integer workIntervalSeconds = null;
        Integer restIntervalSeconds = null;

        switch (sectionType) {
            case AMRAP, FOR_TIME -> {
                timeCapMinutes = expectIntPrefix(lines, cursor, "TIME_CAP_MINUTES: ",
                        "time cap minutes for " + sectionType);
            }
            case EMOM -> {
                intervalSeconds = expectIntPrefix(lines, cursor, "INTERVAL_SECONDS: ",
                        "interval seconds for EMOM");
                totalRounds = expectIntPrefix(lines, cursor, "TOTAL_ROUNDS: ",
                        "total rounds for EMOM");
            }
            case TABATA -> {
                workIntervalSeconds = expectIntPrefix(lines, cursor, "WORK_INTERVAL_SECONDS: ",
                        "work interval seconds for TABATA");
                restIntervalSeconds = expectIntPrefix(lines, cursor, "REST_INTERVAL_SECONDS: ",
                        "rest interval seconds for TABATA");
                totalRounds = expectIntPrefix(lines, cursor, "TOTAL_ROUNDS: ",
                        "total rounds for TABATA");
            }
            case STRENGTH, ACCESSORY -> {
                // No section-level timing fields
            }
        }

        // Parse exercises
        List<Exercise> exercises = new ArrayList<>();
        while (cursor[0] < lines.size()) {
            String line = lines.get(cursor[0]);
            if (line.isBlank()) break;
            if (SECTION_HEADER_PATTERN.matcher(line).matches()) break;
            if (DAY_HEADER_PATTERN.matcher(line).matches()) break;
            if (line.startsWith("WORKOUT: ") || line.startsWith("PROGRAM: ")) break;

            if (line.startsWith("- ")) {
                exercises.add(parseExerciseLine(line, cursor[0]));
                cursor[0]++;
            } else {
                throw new ParsingException("Expected exercise line at line " + (cursor[0] + 1)
                        + ", got: " + line);
            }
        }

        if (exercises.isEmpty()) {
            throw new ParsingException("Section '" + sectionName + "' must contain at least one exercise");
        }

        try {
            return new Section(sectionName, sectionType, exercises,
                    timeCapMinutes, intervalSeconds, totalRounds,
                    workIntervalSeconds, restIntervalSeconds);
        } catch (IllegalArgumentException e) {
            throw new ParsingException("Invalid section '" + sectionName + "': " + e.getMessage(), e);
        }
    }

    // ---- Exercise parsing ----

    private static Exercise parseExerciseLine(String line, int lineIndex) throws ParsingException {
        Matcher matcher = EXERCISE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new ParsingException("Invalid exercise format at line " + (lineIndex + 1)
                    + ": " + line);
        }

        String name = matcher.group(1).trim();
        int sets;
        try {
            sets = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException e) {
            throw new ParsingException("Invalid sets value at line " + (lineIndex + 1)
                    + ": " + matcher.group(2), e);
        }

        String reps = matcher.group(3).trim();
        String weight = matcher.group(4) != null ? matcher.group(4).trim() : null;
        Integer restSeconds = null;
        if (matcher.group(5) != null) {
            try {
                restSeconds = Integer.parseInt(matcher.group(5));
            } catch (NumberFormatException e) {
                throw new ParsingException("Invalid rest seconds at line " + (lineIndex + 1)
                        + ": " + matcher.group(5), e);
            }
        }

        try {
            return new Exercise(name, sets, reps, weight, restSeconds);
        } catch (IllegalArgumentException e) {
            throw new ParsingException("Invalid exercise at line " + (lineIndex + 1)
                    + ": " + e.getMessage(), e);
        }
    }

    // ---- Helper methods ----

    private static String expectPrefix(List<String> lines, int[] cursor,
                                       String prefix, String fieldDescription) throws ParsingException {
        if (cursor[0] >= lines.size()) {
            throw new ParsingException("Unexpected end of input while looking for " + fieldDescription);
        }
        String line = lines.get(cursor[0]);
        if (!line.startsWith(prefix)) {
            throw new ParsingException("Expected " + fieldDescription + " (prefix '" + prefix
                    + "') at line " + (cursor[0] + 1) + ", got: " + line);
        }
        cursor[0]++;
        return line.substring(prefix.length()).trim();
    }

    private static int expectIntPrefix(List<String> lines, int[] cursor,
                                       String prefix, String fieldDescription) throws ParsingException {
        String value = expectPrefix(lines, cursor, prefix, fieldDescription);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ParsingException("Invalid integer for " + fieldDescription
                    + " at line " + cursor[0] + ": " + value, e);
        }
    }

    private static void skipBlankLines(List<String> lines, int[] cursor) {
        while (cursor[0] < lines.size() && lines.get(cursor[0]).isBlank()) {
            cursor[0]++;
        }
    }

    private static GenerationScope parseScope(String value) throws ParsingException {
        try {
            return GenerationScope.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new ParsingException("Invalid scope: " + value
                    + ". Expected one of: DAY, WEEK, FOUR_WEEK", e);
        }
    }

    private static TrainingStyle parseTrainingStyle(String value) throws ParsingException {
        try {
            return TrainingStyle.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new ParsingException("Invalid training style: " + value
                    + ". Expected one of: CROSSFIT, HYPERTROPHY, STRENGTH", e);
        }
    }

    private static List<TrainingStyle> parseTrainingStyles(String value) throws ParsingException {
        List<TrainingStyle> styles = new ArrayList<>();
        for (String part : value.split(",")) {
            styles.add(parseTrainingStyle(part.trim()));
        }
        if (styles.isEmpty()) {
            throw new ParsingException("Training styles must not be empty");
        }
        return styles;
    }

    private static SectionType parseSectionType(String value) throws ParsingException {
        try {
            return SectionType.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new ParsingException("Invalid section type: " + value
                    + ". Expected one of: STRENGTH, AMRAP, EMOM, TABATA, FOR_TIME, ACCESSORY", e);
        }
    }
}
