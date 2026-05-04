package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses and validates a raw JSON string against the Upload_Schema.
 * <p>
 * Pure domain class — zero Spring/JPA/framework imports.
 * Jackson's {@link ObjectMapper} is used only as a JSON tree parser, not as a
 * framework component; it carries no Spring lifecycle or injection annotations.
 * <p>
 * All validation errors are collected before returning so the caller receives
 * the full set of problems in a single pass.
 */
public class UploadParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Parse {@code rawJson} and validate it against the Upload_Schema.
     *
     * @param rawJson the raw JSON string from the upload request body
     * @return {@link ParseResult.Success} with the mapped {@link Program}, or
     *         {@link ParseResult.Failure} with a non-empty list of errors
     */
    public ParseResult parse(String rawJson) {
        JsonNode root;
        try {
            root = MAPPER.readTree(rawJson);
        } catch (Exception e) {
            return new ParseResult.Failure(List.of(
                    new UploadValidationError("$", "Uploaded file is not valid JSON")));
        }

        List<UploadValidationError> errors = new ArrayList<>();

        // ── program_metadata ────────────────────────────────────────────────
        JsonNode meta = root.path("program_metadata");
        if (meta.isMissingNode() || meta.isNull()) {
            errors.add(new UploadValidationError("program_metadata", "program_metadata is required"));
            return new ParseResult.Failure(errors);
        }

        String programName = requireNonBlankText(meta, "program_name", "program_metadata.program_name", errors);
        String goal        = requireNonBlankText(meta, "goal",         "program_metadata.goal",         errors);
        String version     = requireNonBlankText(meta, "version",      "program_metadata.version",      errors);

        if (version != null && !"1.0".equals(version)) {
            errors.add(new UploadValidationError(
                    "program_metadata.version", "must be \"1.0\""));
        }

        int durationWeeks = 0;
        JsonNode durationNode = meta.path("duration_weeks");
        if (durationNode.isMissingNode() || durationNode.isNull()) {
            errors.add(new UploadValidationError(
                    "program_metadata.duration_weeks", "duration_weeks is required"));
        } else if (!durationNode.isInt()) {
            errors.add(new UploadValidationError(
                    "program_metadata.duration_weeks", "must be an integer"));
        } else {
            durationWeeks = durationNode.intValue();
            if (durationWeeks != 1 && durationWeeks != 4) {
                errors.add(new UploadValidationError(
                        "program_metadata.duration_weeks", "must be 1 or 4"));
            }
        }

        List<String> equipmentProfile = new ArrayList<>();
        JsonNode equipNode = meta.path("equipment_profile");
        if (equipNode.isMissingNode() || equipNode.isNull() || !equipNode.isArray()) {
            errors.add(new UploadValidationError(
                    "program_metadata.equipment_profile", "equipment_profile must be a non-empty array"));
        } else {
            for (int i = 0; i < equipNode.size(); i++) {
                String entry = equipNode.get(i).asText("").trim();
                if (entry.isEmpty()) {
                    errors.add(new UploadValidationError(
                            "program_metadata.equipment_profile[" + i + "]",
                            "equipment entry must not be blank"));
                } else {
                    equipmentProfile.add(entry);
                }
            }
            if (equipNode.size() == 0) {
                errors.add(new UploadValidationError(
                        "program_metadata.equipment_profile",
                        "equipment_profile must contain at least one entry"));
            }
        }

        // ── program_structure ───────────────────────────────────────────────
        JsonNode structure = root.path("program_structure");
        if (structure.isMissingNode() || structure.isNull() || !structure.isArray()) {
            errors.add(new UploadValidationError(
                    "program_structure", "program_structure must be an array"));
            return new ParseResult.Failure(errors);
        }

        if (durationWeeks > 0 && structure.size() != durationWeeks) {
            errors.add(new UploadValidationError(
                    "program_structure",
                    "number of weeks does not match duration_weeks"));
        }

        // Fail fast if metadata errors prevent meaningful structure validation
        if (!errors.isEmpty()) {
            return new ParseResult.Failure(errors);
        }

        List<Week> weeks = new ArrayList<>();
        Set<Integer> seenWeekNumbers = new HashSet<>();

        for (int wi = 0; wi < structure.size(); wi++) {
            JsonNode weekNode = structure.get(wi);
            String weekPath = "program_structure[" + wi + "]";

            JsonNode weekNumNode = weekNode.path("week_number");
            if (weekNumNode.isMissingNode() || !weekNumNode.isInt()) {
                errors.add(new UploadValidationError(weekPath + ".week_number", "week_number is required and must be an integer"));
                continue;
            }
            int weekNumber = weekNumNode.intValue();
            if (weekNumber < 1 || weekNumber > durationWeeks) {
                errors.add(new UploadValidationError(
                        weekPath + ".week_number",
                        "week_number must be within [1, " + durationWeeks + "]"));
            }
            if (!seenWeekNumbers.add(weekNumber)) {
                errors.add(new UploadValidationError(
                        weekPath + ".week_number",
                        "week_number " + weekNumber + " is duplicated"));
            }

            JsonNode daysNode = weekNode.path("days");
            if (daysNode.isMissingNode() || !daysNode.isArray() || daysNode.size() == 0) {
                errors.add(new UploadValidationError(weekPath + ".days", "days must be a non-empty array"));
                weeks.add(new Week(weekNumber, List.of()));
                continue;
            }

            List<Day> days = parseDays(daysNode, weekPath, errors);
            weeks.add(new Week(weekNumber, days));
        }

        if (!errors.isEmpty()) {
            return new ParseResult.Failure(errors);
        }

        Program program = new Program(programName, durationWeeks, goal, equipmentProfile, weeks);
        return new ParseResult.Success(program);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private List<Day> parseDays(JsonNode daysNode, String weekPath, List<UploadValidationError> errors) {
        List<Day> days = new ArrayList<>();
        Set<Integer> seenDayNumbers = new HashSet<>();

        for (int di = 0; di < daysNode.size(); di++) {
            JsonNode dayNode = daysNode.get(di);
            String dayPath = weekPath + ".days[" + di + "]";

            JsonNode dayNumNode = dayNode.path("day_number");
            if (dayNumNode.isMissingNode() || !dayNumNode.isInt()) {
                errors.add(new UploadValidationError(dayPath + ".day_number", "day_number is required and must be an integer"));
                continue;
            }
            int dayNumber = dayNumNode.intValue();
            if (dayNumber < 1 || dayNumber > 7) {
                errors.add(new UploadValidationError(dayPath + ".day_number", "day_number must be within [1, 7]"));
            }
            if (!seenDayNumbers.add(dayNumber)) {
                errors.add(new UploadValidationError(dayPath + ".day_number", "day_number " + dayNumber + " is duplicated within this week"));
            }

            String label       = requireNonBlankText(dayNode, "day_label",   dayPath + ".day_label",   errors);
            String focusArea   = requireNonBlankText(dayNode, "focus_area",  dayPath + ".focus_area",  errors);
            String modalityStr = requireNonBlankText(dayNode, "modality",    dayPath + ".modality",    errors);

            Modality modality = null;
            if (modalityStr != null) {
                modality = parseModality(modalityStr, dayPath + ".modality", errors);
            }

            List<WarmCoolEntry> warmUp   = parseWarmCoolEntries(dayNode.path("warm_up"),   dayPath + ".warm_up",   errors);
            List<WarmCoolEntry> coolDown = parseWarmCoolEntries(dayNode.path("cool_down"),  dayPath + ".cool_down", errors);

            JsonNode blocksNode = dayNode.path("blocks");
            if (blocksNode.isMissingNode() || !blocksNode.isArray() || blocksNode.size() == 0) {
                errors.add(new UploadValidationError(dayPath + ".blocks", "blocks must be a non-empty array"));
                continue;
            }

            List<Section> sections = parseSections(blocksNode, dayPath, modality, errors);
            String methodologySource = dayNode.path("methodology_source").asText(null);
            if (methodologySource != null && methodologySource.isBlank()) {
                methodologySource = null;
            }

            if (label != null && focusArea != null && modality != null) {
                days.add(new Day(dayNumber, label, focusArea, modality,
                        warmUp, sections, coolDown, methodologySource));
            }
        }
        return days;
    }

    private List<Section> parseSections(JsonNode blocksNode, String dayPath,
                                        Modality modality, List<UploadValidationError> errors) {
        List<Section> sections = new ArrayList<>();

        for (int bi = 0; bi < blocksNode.size(); bi++) {
            JsonNode blockNode = blocksNode.get(bi);
            String blockPath = dayPath + ".blocks[" + bi + "]";

            String blockType = requireNonBlankText(blockNode, "block_type", blockPath + ".block_type", errors);
            String format    = requireNonBlankText(blockNode, "format",     blockPath + ".format",     errors);

            Integer timeCap = null;
            JsonNode timeCapNode = blockNode.path("time_cap_minutes");
            if (!timeCapNode.isMissingNode() && !timeCapNode.isNull()) {
                if (!timeCapNode.isInt() || timeCapNode.intValue() < 1) {
                    errors.add(new UploadValidationError(blockPath + ".time_cap_minutes", "time_cap_minutes must be an integer >= 1"));
                } else {
                    timeCap = timeCapNode.intValue();
                }
            }

            JsonNode movementsNode = blockNode.path("movements");
            if (movementsNode.isMissingNode() || !movementsNode.isArray() || movementsNode.size() == 0) {
                errors.add(new UploadValidationError(blockPath + ".movements", "movements must be a non-empty array"));
                continue;
            }

            List<Exercise> exercises = parseExercises(movementsNode, blockPath, modality, errors);

            SectionType sectionType = mapFormatToSectionType(format);

            if (blockType != null && format != null) {
                sections.add(new Section(blockType, sectionType, format, timeCap, exercises));
            }
        }
        return sections;
    }

    private List<Exercise> parseExercises(JsonNode movementsNode, String blockPath,
                                          Modality modality, List<UploadValidationError> errors) {
        List<Exercise> exercises = new ArrayList<>();

        for (int mi = 0; mi < movementsNode.size(); mi++) {
            JsonNode mvNode = movementsNode.get(mi);
            String mvPath = blockPath + ".movements[" + mi + "]";

            String exerciseName = requireNonBlankText(mvNode, "exercise_name", mvPath + ".exercise_name", errors);
            String reps         = requireNonBlankText(mvNode, "prescribed_reps", mvPath + ".prescribed_reps", errors);

            int sets = 0;
            JsonNode setsNode = mvNode.path("prescribed_sets");
            if (setsNode.isMissingNode() || setsNode.isNull()) {
                errors.add(new UploadValidationError(mvPath + ".prescribed_sets", "prescribed_sets is required"));
            } else if (!setsNode.isInt() || setsNode.intValue() < 1) {
                errors.add(new UploadValidationError(mvPath + ".prescribed_sets", "prescribed_sets must be an integer >= 1"));
            } else {
                sets = setsNode.intValue();
            }

            ModalityType modalityType = null;
            JsonNode modalityTypeNode = mvNode.path("modality_type");
            boolean modalityTypePresent = !modalityTypeNode.isMissingNode() && !modalityTypeNode.isNull();

            if (modality == Modality.CROSSFIT) {
                if (!modalityTypePresent) {
                    errors.add(new UploadValidationError(
                            mvPath + ".modality_type",
                            "modality_type is required when day modality is CrossFit"));
                } else {
                    modalityType = parseModalityType(modalityTypeNode.asText(), mvPath + ".modality_type", errors);
                }
            } else if (modalityTypePresent) {
                // Optional for Hypertrophy — parse if present
                modalityType = parseModalityType(modalityTypeNode.asText(), mvPath + ".modality_type", errors);
            }

            String weight      = nullableText(mvNode, "prescribed_weight");
            Integer restSecs   = nullableInt(mvNode, "rest_interval_seconds", mvPath + ".rest_interval_seconds", 0, errors);
            String notes       = nullableText(mvNode, "notes");

            if (exerciseName != null && reps != null && sets > 0) {
                exercises.add(new Exercise(exerciseName, modalityType, sets, reps, weight, restSecs, notes));
            }
        }
        return exercises;
    }

    private List<WarmCoolEntry> parseWarmCoolEntries(JsonNode node, String path,
                                                     List<UploadValidationError> errors) {
        List<WarmCoolEntry> entries = new ArrayList<>();
        if (node.isMissingNode() || node.isNull()) {
            return entries; // warm_up / cool_down are not required to be non-empty
        }
        if (!node.isArray()) {
            errors.add(new UploadValidationError(path, "must be an array"));
            return entries;
        }
        for (int i = 0; i < node.size(); i++) {
            JsonNode entry = node.get(i);
            String entryPath = path + "[" + i + "]";
            String movement    = requireNonBlankText(entry, "movement",    entryPath + ".movement",    errors);
            String instruction = requireNonBlankText(entry, "instruction", entryPath + ".instruction", errors);
            if (movement != null && instruction != null) {
                entries.add(new WarmCoolEntry(movement, instruction));
            }
        }
        return entries;
    }

    // ── field helpers ────────────────────────────────────────────────────────

    /** Returns the text value if present and non-blank, otherwise records an error and returns null. */
    private String requireNonBlankText(JsonNode node, String fieldName, String errorPath,
                                       List<UploadValidationError> errors) {
        JsonNode child = node.path(fieldName);
        if (child.isMissingNode() || child.isNull()) {
            errors.add(new UploadValidationError(errorPath, fieldName + " is required"));
            return null;
        }
        String value = child.asText("").trim();
        if (value.isEmpty()) {
            errors.add(new UploadValidationError(errorPath, fieldName + " must not be blank"));
            return null;
        }
        return value;
    }

    /** Returns the text value if present, or null if absent/null. Does not add errors. */
    private String nullableText(JsonNode node, String fieldName) {
        JsonNode child = node.path(fieldName);
        if (child.isMissingNode() || child.isNull()) return null;
        String value = child.asText("").trim();
        return value.isEmpty() ? null : value;
    }

    /** Returns an integer if present and >= minValue, or null if absent/null. Records an error if present but invalid. */
    private Integer nullableInt(JsonNode node, String fieldName, String errorPath,
                                int minValue, List<UploadValidationError> errors) {
        JsonNode child = node.path(fieldName);
        if (child.isMissingNode() || child.isNull()) return null;
        if (!child.isInt() || child.intValue() < minValue) {
            errors.add(new UploadValidationError(errorPath, fieldName + " must be an integer >= " + minValue));
            return null;
        }
        return child.intValue();
    }

    private Modality parseModality(String value, String errorPath, List<UploadValidationError> errors) {
        return switch (value) {
            case "CrossFit"    -> Modality.CROSSFIT;
            case "Hypertrophy" -> Modality.HYPERTROPHY;
            default -> {
                errors.add(new UploadValidationError(errorPath, "modality must be one of: CrossFit, Hypertrophy"));
                yield null;
            }
        };
    }

    private ModalityType parseModalityType(String value, String errorPath, List<UploadValidationError> errors) {
        return switch (value) {
            case "Engine"       -> ModalityType.ENGINE;
            case "Gymnastics"   -> ModalityType.GYMNASTICS;
            case "Weightlifting" -> ModalityType.WEIGHTLIFTING;
            default -> {
                errors.add(new UploadValidationError(errorPath,
                        "modality_type must be one of: Engine, Gymnastics, Weightlifting"));
                yield null;
            }
        };
    }

    /**
     * Maps the Upload_Schema {@code format} string to the closest {@link SectionType}.
     * Unrecognised formats fall back to {@link SectionType#STRENGTH} — the format string
     * is preserved on the {@link Section} object so no information is lost.
     */
    private SectionType mapFormatToSectionType(String format) {
        if (format == null) return SectionType.STRENGTH;
        return switch (format.toUpperCase()) {
            case "AMRAP"                -> SectionType.AMRAP;
            case "EMOM"                 -> SectionType.EMOM;
            case "TABATA"               -> SectionType.TABATA;
            case "RFT", "FOR TIME"      -> SectionType.FOR_TIME;
            case "ACCESSORY"            -> SectionType.ACCESSORY;
            default                     -> SectionType.STRENGTH;
        };
    }
}
