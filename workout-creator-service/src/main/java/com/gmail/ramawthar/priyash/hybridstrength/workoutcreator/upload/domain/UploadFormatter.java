package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;

/**
 * Serialises a {@link Program} domain object into a canonical JSON string
 * conforming to the Upload_Schema.
 * <p>
 * Pure domain class — zero Spring/JPA/framework imports.
 * This is the inverse of {@link UploadParser}: {@code parse(format(parse(json))) ≡ parse(json)}.
 */
public class UploadFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Serialise {@code program} to a canonical Upload_Schema JSON string.
     *
     * @param program the domain object to serialise
     * @return a JSON string conforming to the Upload_Schema
     * @throws IllegalStateException if serialisation fails (should not occur for valid domain objects)
     */
    public String format(Program program) {
        ObjectNode root = MAPPER.createObjectNode();

        // ── program_metadata ────────────────────────────────────────────────
        ObjectNode meta = root.putObject("program_metadata");
        meta.put("program_name",    program.getName());
        meta.put("duration_weeks",  program.getDurationWeeks());
        meta.put("goal",            program.getGoal());
        meta.put("version",         "1.0");

        ArrayNode equipArray = meta.putArray("equipment_profile");
        program.getEquipmentProfile().forEach(equipArray::add);

        // ── program_structure ───────────────────────────────────────────────
        ArrayNode structure = root.putArray("program_structure");

        for (Week week : program.getWeeks()) {
            ObjectNode weekNode = structure.addObject();
            weekNode.put("week_number", week.getWeekNumber());

            ArrayNode daysArray = weekNode.putArray("days");
            for (Day day : week.getDays()) {
                ObjectNode dayNode = daysArray.addObject();
                dayNode.put("day_number",  day.getDayNumber());
                dayNode.put("day_label",   day.getLabel());
                dayNode.put("focus_area",  day.getFocusArea());
                dayNode.put("modality",    formatModality(day.getModality()));

                // warm_up
                ArrayNode warmUpArray = dayNode.putArray("warm_up");
                for (WarmCoolEntry entry : day.getWarmUp()) {
                    ObjectNode e = warmUpArray.addObject();
                    e.put("movement",    entry.movement());
                    e.put("instruction", entry.instruction());
                }

                // blocks
                ArrayNode blocksArray = dayNode.putArray("blocks");
                for (Section section : day.getSections()) {
                    ObjectNode blockNode = blocksArray.addObject();
                    blockNode.put("block_type", section.getName());
                    blockNode.put("format",     section.getFormat());
                    if (section.getTimeCap() != null) {
                        blockNode.put("time_cap_minutes", section.getTimeCap());
                    }

                    ArrayNode movementsArray = blockNode.putArray("movements");
                    for (Exercise exercise : section.getExercises()) {
                        ObjectNode mvNode = movementsArray.addObject();
                        mvNode.put("exercise_name",    exercise.getName());
                        if (exercise.getModalityType() != null) {
                            mvNode.put("modality_type", formatModalityType(exercise.getModalityType()));
                        }
                        mvNode.put("prescribed_sets",  exercise.getSets());
                        mvNode.put("prescribed_reps",  exercise.getReps());
                        if (exercise.getWeight() != null) {
                            mvNode.put("prescribed_weight", exercise.getWeight());
                        }
                        if (exercise.getRestSeconds() != null) {
                            mvNode.put("rest_interval_seconds", exercise.getRestSeconds());
                        }
                        if (exercise.getNotes() != null) {
                            mvNode.put("notes", exercise.getNotes());
                        }
                    }
                }

                // cool_down
                ArrayNode coolDownArray = dayNode.putArray("cool_down");
                for (WarmCoolEntry entry : day.getCoolDown()) {
                    ObjectNode e = coolDownArray.addObject();
                    e.put("movement",    entry.movement());
                    e.put("instruction", entry.instruction());
                }

                if (day.getMethodologySource() != null) {
                    dayNode.put("methodology_source", day.getMethodologySource());
                }
            }
        }

        try {
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialise Program to JSON", e);
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private String formatModality(Modality modality) {
        return switch (modality) {
            case CROSSFIT    -> "CrossFit";
            case HYPERTROPHY -> "Hypertrophy";
        };
    }

    private String formatModalityType(ModalityType modalityType) {
        return switch (modalityType) {
            case ENGINE       -> "Engine";
            case GYMNASTICS   -> "Gymnastics";
            case WEIGHTLIFTING -> "Weightlifting";
        };
    }
}
