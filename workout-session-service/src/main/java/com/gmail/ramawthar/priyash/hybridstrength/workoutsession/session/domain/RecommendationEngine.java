package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure domain service that computes exercise recommendations from the workout snapshot.
 * No framework dependencies — testable with plain JUnit and jqwik.
 */
public class RecommendationEngine {

    private static final int MAX_STRING_LENGTH = 50;
    private static final int MAX_SETS = 100;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Computes recommendations for ALL exercises across all sections in the given day.
     *
     * @param workoutSnapshot the raw JSON program snapshot from the Session
     * @param weekNumber      the week number (1-based) for navigation within the snapshot
     * @param dayNumber       the day number (1-based) for navigation within the snapshot
     * @return ordered list of ExerciseRecommendation grouped by section, then exercise index
     */
    public List<ExerciseRecommendation> computeAll(String workoutSnapshot, int weekNumber, int dayNumber) {
        JsonNode dayNode = parseDayNode(workoutSnapshot, weekNumber, dayNumber);
        if (dayNode == null) {
            return Collections.emptyList();
        }

        String modality = extractModality(dayNode);
        JsonNode sectionsNode = dayNode.get("sections");
        if (sectionsNode == null || !sectionsNode.isArray()) {
            return Collections.emptyList();
        }

        List<ExerciseRecommendation> recommendations = new ArrayList<>();
        for (int sectionIdx = 0; sectionIdx < sectionsNode.size(); sectionIdx++) {
            JsonNode sectionNode = sectionsNode.get(sectionIdx);
            recommendations.addAll(extractSectionRecommendations(sectionNode, sectionIdx, modality));
        }
        return recommendations;
    }

    /**
     * Computes recommendations for exercises in a single section.
     *
     * @param workoutSnapshot the raw JSON program snapshot
     * @param weekNumber      the week number (1-based)
     * @param dayNumber       the day number (1-based)
     * @param sectionIndex    the target section index (0-based)
     * @return ordered list of ExerciseRecommendation for the specified section
     */
    public List<ExerciseRecommendation> computeForSection(String workoutSnapshot, int weekNumber,
                                                           int dayNumber, int sectionIndex) {
        JsonNode dayNode = parseDayNode(workoutSnapshot, weekNumber, dayNumber);
        if (dayNode == null) {
            return Collections.emptyList();
        }

        String modality = extractModality(dayNode);
        JsonNode sectionsNode = dayNode.get("sections");
        if (sectionsNode == null || !sectionsNode.isArray()) {
            return Collections.emptyList();
        }

        if (sectionIndex < 0 || sectionIndex >= sectionsNode.size()) {
            // Out-of-bounds section → return empty list (no exercises to produce all-null for)
            return Collections.emptyList();
        }

        JsonNode sectionNode = sectionsNode.get(sectionIndex);
        return extractSectionRecommendations(sectionNode, sectionIndex, modality);
    }

    private JsonNode parseDayNode(String workoutSnapshot, int weekNumber, int dayNumber) {
        JsonNode root;
        try {
            root = objectMapper.readTree(workoutSnapshot);
        } catch (Exception e) {
            throw new SnapshotParseException("Failed to parse workout snapshot JSON", e);
        }

        if (root == null || !root.isObject()) {
            throw new SnapshotParseException("Workout snapshot is not a valid JSON object");
        }

        JsonNode weeksNode = root.get("weeks");
        if (weeksNode == null || !weeksNode.isArray()) {
            return null;
        }

        int weekIndex = weekNumber - 1;
        if (weekIndex < 0 || weekIndex >= weeksNode.size()) {
            return null;
        }

        JsonNode weekNode = weeksNode.get(weekIndex);
        JsonNode daysNode = weekNode.get("days");
        if (daysNode == null || !daysNode.isArray()) {
            return null;
        }

        int dayIndex = dayNumber - 1;
        if (dayIndex < 0 || dayIndex >= daysNode.size()) {
            return null;
        }

        return daysNode.get(dayIndex);
    }

    private String extractModality(JsonNode dayNode) {
        JsonNode modalityNode = dayNode.get("modality");
        if (modalityNode == null || modalityNode.isNull()) {
            return "HYPERTROPHY"; // default to HYPERTROPHY if not specified
        }
        return modalityNode.asText();
    }

    private List<ExerciseRecommendation> extractSectionRecommendations(JsonNode sectionNode,
                                                                        int sectionIndex,
                                                                        String modality) {
        JsonNode exercisesNode = sectionNode.get("exercises");
        if (exercisesNode == null || !exercisesNode.isArray()) {
            return Collections.emptyList();
        }

        List<ExerciseRecommendation> recommendations = new ArrayList<>();
        for (int exerciseIdx = 0; exerciseIdx < exercisesNode.size(); exerciseIdx++) {
            JsonNode exerciseNode = exercisesNode.get(exerciseIdx);
            ExerciseRecommendation rec = buildRecommendation(exerciseNode, sectionIndex, exerciseIdx, modality);
            recommendations.add(rec);
        }
        return recommendations;
    }

    private ExerciseRecommendation buildRecommendation(JsonNode exerciseNode, int sectionIndex,
                                                        int exerciseIndex, String modality) {
        String weight = extractStringField(exerciseNode, "weight");
        String reps = extractStringField(exerciseNode, "reps");
        Integer sets = extractSetsField(exerciseNode);

        // Apply truncation and clamping
        weight = truncateString(weight);
        reps = truncateString(reps);
        sets = clampSets(sets);

        if ("CROSSFIT".equalsIgnoreCase(modality)) {
            // CROSSFIT: only weight is populated
            return new ExerciseRecommendation(sectionIndex, exerciseIndex, weight, null, null);
        }

        // HYPERTROPHY (default): populate all three fields; sets == 0 → null
        return new ExerciseRecommendation(sectionIndex, exerciseIndex, weight, reps, sets);
    }

    private String extractStringField(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText();
        if (value.isBlank()) {
            return null;
        }
        return value;
    }

    private Integer extractSetsField(JsonNode node) {
        JsonNode setsNode = node.get("sets");
        if (setsNode == null || setsNode.isNull()) {
            return null;
        }
        if (!setsNode.isNumber()) {
            // Try to parse as integer from text
            try {
                int value = Integer.parseInt(setsNode.asText());
                return value == 0 ? null : value;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        int value = setsNode.intValue();
        return value == 0 ? null : value;
    }

    private String truncateString(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() > MAX_STRING_LENGTH) {
            return value.substring(0, MAX_STRING_LENGTH);
        }
        return value;
    }

    private Integer clampSets(Integer sets) {
        if (sets == null) {
            return null;
        }
        if (sets > MAX_SETS) {
            return MAX_SETS;
        }
        return sets;
    }
}
