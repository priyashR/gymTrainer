package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Constructs a structured prompt string from the user's description, scope,
 * and training styles for submission to the Gemini AI service.
 * <p>
 * The prompt includes:
 * <ul>
 *   <li>The user's natural language description</li>
 *   <li>All requested training style names</li>
 *   <li>The generation scope (day / week / 4-week)</li>
 *   <li>Schema constraints for the expected Gemini response format</li>
 * </ul>
 * Pure Java — no framework imports.
 */
public final class PromptBuilder {

    private PromptBuilder() {
        // Utility class — not instantiable
    }

    /**
     * Builds a structured prompt for Gemini from the given generation parameters.
     *
     * @param description    the user's natural language description; must not be null or blank
     * @param scope          the generation scope; must not be null
     * @param trainingStyles the requested training styles; must not be null or empty
     * @return the structured prompt string
     */
    public static String buildPrompt(String description, GenerationScope scope,
                                     List<TrainingStyle> trainingStyles) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description must not be null or blank");
        }
        Objects.requireNonNull(scope, "Scope must not be null");
        if (trainingStyles == null || trainingStyles.isEmpty()) {
            throw new IllegalArgumentException("Training styles must not be null or empty");
        }

        String styleNames = trainingStyles.stream()
                .map(TrainingStyle::name)
                .collect(Collectors.joining(", "));

        StringBuilder prompt = new StringBuilder();

        prompt.append("Generate a ").append(scopeLabel(scope)).append(" for the following request.\n\n");
        prompt.append("Description: ").append(description.trim()).append("\n");
        prompt.append("Training Style(s): ").append(styleNames).append("\n");
        prompt.append("Scope: ").append(scope.name()).append("\n\n");

        prompt.append("Response Format Constraints:\n");
        prompt.append("- Respond in plain text only. Do not use HTML or markdown formatting.\n");

        appendScopeInstructions(prompt, scope);
        appendSchemaConstraints(prompt);

        return prompt.toString();
    }

    private static String scopeLabel(GenerationScope scope) {
        return switch (scope) {
            case DAY -> "single-day workout";
            case WEEK -> "7-day training program";
            case FOUR_WEEK -> "28-day (4-week) training program";
        };
    }

    private static void appendScopeInstructions(StringBuilder prompt, GenerationScope scope) {
        switch (scope) {
            case DAY -> prompt.append("- Generate exactly one workout for a single day.\n");
            case WEEK -> prompt.append("- Generate a program with one workout per day for 7 days.\n");
            case FOUR_WEEK -> prompt.append("- Generate a program with one workout per day for 28 days (4 weeks).\n");
        }
    }

    private static void appendSchemaConstraints(StringBuilder prompt) {
        prompt.append("- Each workout must have a name, description, and one or more sections.\n");
        prompt.append("- Each section must have a name and a type from: STRENGTH, AMRAP, EMOM, TABATA, FOR_TIME, ACCESSORY.\n");
        prompt.append("- Each section must contain one or more exercises.\n");
        prompt.append("- Each exercise must have a name, sets (integer), and reps (string, e.g. \"8-12\" or \"max\").\n");
        prompt.append("- Each exercise may optionally have a weight (string, e.g. \"135 lbs\" or \"bodyweight\").\n");
        prompt.append("- For STRENGTH and ACCESSORY sections, each exercise must include restSeconds (positive integer).\n");
        prompt.append("- For AMRAP and FOR_TIME sections, include timeCapMinutes (positive integer) at the section level.\n");
        prompt.append("- For EMOM sections, include intervalSeconds and totalRounds (positive integers) at the section level.\n");
        prompt.append("- For TABATA sections, include workIntervalSeconds, restIntervalSeconds, and totalRounds (positive integers) at the section level.\n");
        prompt.append("- Do not include per-exercise restSeconds for timed section types (AMRAP, EMOM, TABATA, FOR_TIME).\n");
    }
}
