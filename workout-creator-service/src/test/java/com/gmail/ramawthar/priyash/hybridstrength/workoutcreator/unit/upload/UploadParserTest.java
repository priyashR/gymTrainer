package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit.upload;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Modality;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ModalityType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Program;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UploadParser}.
 * Each test exercises a single validation rule in isolation.
 * Naming convention: parse_StateUnderTest_ExpectedBehaviour
 */
class UploadParserTest {

    private UploadParser parser;

    @BeforeEach
    void setUp() {
        parser = new UploadParser();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns a minimal but fully valid 1-week Hypertrophy program JSON string.
     * Each test starts from this and mutates one field to trigger a specific error.
     */
    private String validProgramJson() {
        return """
                {
                  "program_metadata": {
                    "program_name": "Test Program",
                    "duration_weeks": 1,
                    "goal": "Hypertrophy",
                    "equipment_profile": ["Barbell", "Dumbbells"],
                    "version": "1.0"
                  },
                  "program_structure": [
                    {
                      "week_number": 1,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Push",
                          "modality": "Hypertrophy",
                          "warm_up": [{"movement": "Arm circles", "instruction": "10 reps each direction"}],
                          "blocks": [
                            {
                              "block_type": "Tier 1: Compound",
                              "format": "Sets/Reps",
                              "movements": [
                                {
                                  "exercise_name": "Bench Press",
                                  "prescribed_sets": 4,
                                  "prescribed_reps": "8-10"
                                }
                              ]
                            }
                          ],
                          "cool_down": [{"movement": "Chest stretch", "instruction": "Hold 30 seconds"}]
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private void assertFailureWithField(ParseResult result, String expectedField) {
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::field)
                .contains(expectedField);
    }

    // ── failure cases ─────────────────────────────────────────────────────────

    @Test
    void parse_InvalidJson_ReturnsFailureWithRootField() {
        ParseResult result = parser.parse("not valid json {{{");
        assertFailureWithField(result, "$");
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors().get(0).message()).isEqualTo("Uploaded file is not valid JSON");
    }

    @Test
    void parse_MissingProgramMetadata_ReturnsFailureWithProgramMetadataField() {
        String json = """
                {
                  "program_structure": []
                }
                """;
        assertFailureWithField(parser.parse(json), "program_metadata");
    }

    @Test
    void parse_MissingProgramName_ReturnsFailureWithProgramNameField() {
        String json = validProgramJson().replace("\"program_name\": \"Test Program\",", "");
        assertFailureWithField(parser.parse(json), "program_metadata.program_name");
    }

    @Test
    void parse_BlankProgramName_ReturnsFailure() {
        String json = validProgramJson().replace("\"Test Program\"", "\"   \"");
        assertFailureWithField(parser.parse(json), "program_metadata.program_name");
    }

    @Test
    void parse_MissingVersion_ReturnsFailure() {
        String json = validProgramJson().replace("\"version\": \"1.0\"", "\"version\": null");
        assertFailureWithField(parser.parse(json), "program_metadata.version");
    }

    @Test
    void parse_WrongVersion_ReturnsFailureWithVersionFieldAndCorrectMessage() {
        String json = validProgramJson().replace("\"1.0\"", "\"2.0\"");
        ParseResult result = parser.parse(json);
        assertFailureWithField(result, "program_metadata.version");
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .filteredOn(e -> e.field().equals("program_metadata.version"))
                .extracting(UploadValidationError::message)
                .first().asString().contains("1.0");
    }

    @Test
    void parse_MissingDurationWeeks_ReturnsFailure() {
        String json = validProgramJson().replace("\"duration_weeks\": 1,", "");
        assertFailureWithField(parser.parse(json), "program_metadata.duration_weeks");
    }

    @Test
    void parse_DurationWeeksIsTwo_ReturnsFailureWithCorrectMessage() {
        String json = validProgramJson()
                .replace("\"duration_weeks\": 1,", "\"duration_weeks\": 2,")
                .replace("\"week_number\": 1", "\"week_number\": 1");
        ParseResult result = parser.parse(json);
        assertFailureWithField(result, "program_metadata.duration_weeks");
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .filteredOn(e -> e.field().equals("program_metadata.duration_weeks"))
                .extracting(UploadValidationError::message)
                .first().asString().contains("1 or 4");
    }

    @Test
    void parse_DurationWeeksIsThree_ReturnsFailureWithCorrectMessage() {
        String json = validProgramJson().replace("\"duration_weeks\": 1,", "\"duration_weeks\": 3,");
        ParseResult result = parser.parse(json);
        assertFailureWithField(result, "program_metadata.duration_weeks");
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .filteredOn(e -> e.field().equals("program_metadata.duration_weeks"))
                .extracting(UploadValidationError::message)
                .first().asString().contains("1 or 4");
    }

    @Test
    void parse_DurationWeeksIsZero_ReturnsFailure() {
        String json = validProgramJson().replace("\"duration_weeks\": 1,", "\"duration_weeks\": 0,");
        assertFailureWithField(parser.parse(json), "program_metadata.duration_weeks");
    }

    @Test
    void parse_MissingEquipmentProfile_ReturnsFailure() {
        String json = validProgramJson().replace(
                "\"equipment_profile\": [\"Barbell\", \"Dumbbells\"],", "");
        assertFailureWithField(parser.parse(json), "program_metadata.equipment_profile");
    }

    @Test
    void parse_EmptyEquipmentProfile_ReturnsFailureWithCorrectMessage() {
        String json = validProgramJson().replace(
                "\"equipment_profile\": [\"Barbell\", \"Dumbbells\"]",
                "\"equipment_profile\": []");
        ParseResult result = parser.parse(json);
        assertFailureWithField(result, "program_metadata.equipment_profile");
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .filteredOn(e -> e.field().equals("program_metadata.equipment_profile"))
                .extracting(UploadValidationError::message)
                .first().asString().contains("at least one entry");
    }

    @Test
    void parse_ProgramStructureLengthDoesNotMatchDurationWeeks_ReturnsFailureWithCorrectMessage() {
        // duration_weeks=1 but program_structure has 2 weeks
        String json = """
                {
                  "program_metadata": {
                    "program_name": "Test Program",
                    "duration_weeks": 1,
                    "goal": "Hypertrophy",
                    "equipment_profile": ["Barbell"],
                    "version": "1.0"
                  },
                  "program_structure": [
                    {
                      "week_number": 1,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Push",
                          "modality": "Hypertrophy",
                          "blocks": [
                            {
                              "block_type": "Tier 1",
                              "format": "Sets/Reps",
                              "movements": [{"exercise_name": "Squat", "prescribed_sets": 3, "prescribed_reps": "5"}]
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "week_number": 2,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Pull",
                          "modality": "Hypertrophy",
                          "blocks": [
                            {
                              "block_type": "Tier 1",
                              "format": "Sets/Reps",
                              "movements": [{"exercise_name": "Deadlift", "prescribed_sets": 3, "prescribed_reps": "5"}]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
        ParseResult result = parser.parse(json);
        assertFailureWithField(result, "program_structure");
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .filteredOn(e -> e.field().equals("program_structure"))
                .extracting(UploadValidationError::message)
                .first().asString().contains("number of weeks does not match duration_weeks");
    }

    @Test
    void parse_DuplicateWeekNumber_ReturnsFailureWithDuplicatedMessage() {
        // 4-week program with week_number 1 appearing twice
        String json = """
                {
                  "program_metadata": {
                    "program_name": "Test Program",
                    "duration_weeks": 4,
                    "goal": "Hypertrophy",
                    "equipment_profile": ["Barbell"],
                    "version": "1.0"
                  },
                  "program_structure": [
                    {
                      "week_number": 1,
                      "days": [{"day_number": 1, "day_label": "Mon", "focus_area": "Push", "modality": "Hypertrophy",
                        "blocks": [{"block_type": "T1", "format": "Sets/Reps",
                          "movements": [{"exercise_name": "Squat", "prescribed_sets": 3, "prescribed_reps": "5"}]}]}]
                    },
                    {
                      "week_number": 2,
                      "days": [{"day_number": 1, "day_label": "Mon", "focus_area": "Push", "modality": "Hypertrophy",
                        "blocks": [{"block_type": "T1", "format": "Sets/Reps",
                          "movements": [{"exercise_name": "Squat", "prescribed_sets": 3, "prescribed_reps": "5"}]}]}]
                    },
                    {
                      "week_number": 3,
                      "days": [{"day_number": 1, "day_label": "Mon", "focus_area": "Push", "modality": "Hypertrophy",
                        "blocks": [{"block_type": "T1", "format": "Sets/Reps",
                          "movements": [{"exercise_name": "Squat", "prescribed_sets": 3, "prescribed_reps": "5"}]}]}]
                    },
                    {
                      "week_number": 1,
                      "days": [{"day_number": 1, "day_label": "Mon", "focus_area": "Push", "modality": "Hypertrophy",
                        "blocks": [{"block_type": "T1", "format": "Sets/Reps",
                          "movements": [{"exercise_name": "Squat", "prescribed_sets": 3, "prescribed_reps": "5"}]}]}]
                    }
                  ]
                }
                """;
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::message)
                .anyMatch(m -> m.contains("is duplicated"));
    }

    @Test
    void parse_WeekNumberOutOfRange_ReturnsFailureWithCorrectMessage() {
        // duration_weeks=1 but week_number=5
        String json = validProgramJson().replace("\"week_number\": 1", "\"week_number\": 5");
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::message)
                .anyMatch(m -> m.contains("week_number must be within [1,"));
    }

    @Test
    void parse_DuplicateDayNumberWithinWeek_ReturnsFailureWithDuplicatedMessage() {
        String json = """
                {
                  "program_metadata": {
                    "program_name": "Test Program",
                    "duration_weeks": 1,
                    "goal": "Hypertrophy",
                    "equipment_profile": ["Barbell"],
                    "version": "1.0"
                  },
                  "program_structure": [
                    {
                      "week_number": 1,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Push",
                          "modality": "Hypertrophy",
                          "blocks": [{"block_type": "T1", "format": "Sets/Reps",
                            "movements": [{"exercise_name": "Squat", "prescribed_sets": 3, "prescribed_reps": "5"}]}]
                        },
                        {
                          "day_number": 1,
                          "day_label": "Tuesday",
                          "focus_area": "Pull",
                          "modality": "Hypertrophy",
                          "blocks": [{"block_type": "T1", "format": "Sets/Reps",
                            "movements": [{"exercise_name": "Deadlift", "prescribed_sets": 3, "prescribed_reps": "5"}]}]
                        }
                      ]
                    }
                  ]
                }
                """;
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::message)
                .anyMatch(m -> m.contains("is duplicated within this week"));
    }

    @Test
    void parse_DayNumberZero_ReturnsFailureWithCorrectMessage() {
        String json = validProgramJson().replace("\"day_number\": 1,", "\"day_number\": 0,");
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::message)
                .anyMatch(m -> m.contains("day_number must be within [1, 7]"));
    }

    @Test
    void parse_DayNumberEight_ReturnsFailureWithCorrectMessage() {
        String json = validProgramJson().replace("\"day_number\": 1,", "\"day_number\": 8,");
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::message)
                .anyMatch(m -> m.contains("day_number must be within [1, 7]"));
    }

    @Test
    void parse_EmptyBlocksArray_ReturnsFailureWithCorrectMessage() {
        String json = """
                {
                  "program_metadata": {
                    "program_name": "Test Program",
                    "duration_weeks": 1,
                    "goal": "Hypertrophy",
                    "equipment_profile": ["Barbell"],
                    "version": "1.0"
                  },
                  "program_structure": [
                    {
                      "week_number": 1,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Push",
                          "modality": "Hypertrophy",
                          "blocks": []
                        }
                      ]
                    }
                  ]
                }
                """;
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::message)
                .anyMatch(m -> m.contains("blocks must be a non-empty array"));
    }

    @Test
    void parse_EmptyMovementsArray_ReturnsFailureWithCorrectMessage() {
        String json = """
                {
                  "program_metadata": {
                    "program_name": "Test Program",
                    "duration_weeks": 1,
                    "goal": "Hypertrophy",
                    "equipment_profile": ["Barbell"],
                    "version": "1.0"
                  },
                  "program_structure": [
                    {
                      "week_number": 1,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Push",
                          "modality": "Hypertrophy",
                          "blocks": [
                            {
                              "block_type": "Tier 1: Compound",
                              "format": "Sets/Reps",
                              "movements": []
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::message)
                .anyMatch(m -> m.contains("movements must be a non-empty array"));
    }

    @Test
    void parse_MissingModalityTypeOnCrossFitDay_ReturnsFailureWithCorrectMessage() {
        String json = validProgramJson()
                .replace("\"modality\": \"Hypertrophy\"", "\"modality\": \"CrossFit\"");
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::message)
                .anyMatch(m -> m.contains("modality_type is required when day modality is CrossFit"));
    }

    @Test
    void parse_InvalidModalityTypeValue_ReturnsFailureWithCorrectMessage() {
        String json = validProgramJson()
                .replace("\"modality\": \"Hypertrophy\"", "\"modality\": \"CrossFit\"")
                .replace(
                        "\"prescribed_reps\": \"8-10\"",
                        "\"prescribed_reps\": \"8-10\", \"modality_type\": \"InvalidType\"");
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::message)
                .anyMatch(m -> m.contains("modality_type must be one of: Engine, Gymnastics, Weightlifting"));
    }

    @Test
    void parse_InvalidModalityValue_ReturnsFailureWithCorrectMessage() {
        String json = validProgramJson()
                .replace("\"modality\": \"Hypertrophy\"", "\"modality\": \"Powerlifting\"");
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::message)
                .anyMatch(m -> m.contains("modality must be one of: CrossFit, Hypertrophy"));
    }

    @Test
    void parse_PrescribedSetsLessThanOne_ReturnsFailure() {
        String json = validProgramJson().replace("\"prescribed_sets\": 4", "\"prescribed_sets\": 0");
        assertThat(parser.parse(json)).isInstanceOf(ParseResult.Failure.class);
    }

    @Test
    void parse_MissingPrescribedReps_ReturnsFailure() {
        String json = """
                {
                  "program_metadata": {
                    "program_name": "Test Program",
                    "duration_weeks": 1,
                    "goal": "Hypertrophy",
                    "equipment_profile": ["Barbell"],
                    "version": "1.0"
                  },
                  "program_structure": [
                    {
                      "week_number": 1,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Push",
                          "modality": "Hypertrophy",
                          "blocks": [
                            {
                              "block_type": "Tier 1: Compound",
                              "format": "Sets/Reps",
                              "movements": [
                                {
                                  "exercise_name": "Bench Press",
                                  "prescribed_sets": 4
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
        assertThat(parser.parse(json)).isInstanceOf(ParseResult.Failure.class);
    }

    @Test
    void parse_TimeCapMinutesIsZero_ReturnsFailureWithCorrectMessage() {
        String json = validProgramJson().replace(
                "\"prescribed_reps\": \"8-10\"",
                "\"prescribed_reps\": \"8-10\", \"time_cap_minutes\": 0");
        // time_cap_minutes is on the block, not the movement — inject at block level
        String jsonWithTimeCap = validProgramJson().replace(
                "\"format\": \"Sets/Reps\",",
                "\"format\": \"Sets/Reps\", \"time_cap_minutes\": 0,");
        ParseResult result = parser.parse(jsonWithTimeCap);
        assertThat(result).isInstanceOf(ParseResult.Failure.class);
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertThat(failure.errors())
                .extracting(UploadValidationError::message)
                .anyMatch(m -> m.contains("time_cap_minutes must be an integer >= 1"));
    }

    // ── success cases ─────────────────────────────────────────────────────────

    @Test
    void parse_ValidOneWeekHypertrophyProgram_ReturnsSuccessWithCorrectProgramFields() {
        ParseResult result = parser.parse(validProgramJson());

        assertThat(result).isInstanceOf(ParseResult.Success.class);
        Program program = ((ParseResult.Success) result).program();

        assertThat(program.getName()).isEqualTo("Test Program");
        assertThat(program.getDurationWeeks()).isEqualTo(1);
        assertThat(program.getGoal()).isEqualTo("Hypertrophy");
        assertThat(program.getEquipmentProfile()).containsExactly("Barbell", "Dumbbells");
        assertThat(program.getWeeks()).hasSize(1);
        assertThat(program.getWeeks().get(0).getWeekNumber()).isEqualTo(1);
        assertThat(program.getWeeks().get(0).getDays()).hasSize(1);
        assertThat(program.getWeeks().get(0).getDays().get(0).getDayNumber()).isEqualTo(1);
        assertThat(program.getWeeks().get(0).getDays().get(0).getModality()).isEqualTo(Modality.HYPERTROPHY);
    }

    @Test
    void parse_ValidFourWeekCrossFitProgram_ReturnsSuccess() {
        String json = """
                {
                  "program_metadata": {
                    "program_name": "CrossFit 4-Week",
                    "duration_weeks": 4,
                    "goal": "CrossFit",
                    "equipment_profile": ["Barbell"],
                    "version": "1.0"
                  },
                  "program_structure": [
                    {
                      "week_number": 1,
                      "days": [{"day_number": 1, "day_label": "Mon", "focus_area": "Engine", "modality": "CrossFit",
                        "blocks": [{"block_type": "Metcon", "format": "AMRAP",
                          "movements": [{"exercise_name": "Burpees", "prescribed_sets": 1, "prescribed_reps": "AMRAP",
                            "modality_type": "Engine"}]}]}]
                    },
                    {
                      "week_number": 2,
                      "days": [{"day_number": 1, "day_label": "Mon", "focus_area": "Engine", "modality": "CrossFit",
                        "blocks": [{"block_type": "Metcon", "format": "AMRAP",
                          "movements": [{"exercise_name": "Burpees", "prescribed_sets": 1, "prescribed_reps": "AMRAP",
                            "modality_type": "Engine"}]}]}]
                    },
                    {
                      "week_number": 3,
                      "days": [{"day_number": 1, "day_label": "Mon", "focus_area": "Engine", "modality": "CrossFit",
                        "blocks": [{"block_type": "Metcon", "format": "AMRAP",
                          "movements": [{"exercise_name": "Burpees", "prescribed_sets": 1, "prescribed_reps": "AMRAP",
                            "modality_type": "Engine"}]}]}]
                    },
                    {
                      "week_number": 4,
                      "days": [{"day_number": 1, "day_label": "Mon", "focus_area": "Engine", "modality": "CrossFit",
                        "blocks": [{"block_type": "Metcon", "format": "AMRAP",
                          "movements": [{"exercise_name": "Burpees", "prescribed_sets": 1, "prescribed_reps": "AMRAP",
                            "modality_type": "Engine"}]}]}]
                    }
                  ]
                }
                """;
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Success.class);
        Program program = ((ParseResult.Success) result).program();
        assertThat(program.getDurationWeeks()).isEqualTo(4);
        assertThat(program.getWeeks()).hasSize(4);
    }

    @Test
    void parse_ModalityTypeAbsentOnHypertrophyDay_ReturnsSuccess() {
        // The valid base JSON has no modality_type — this test confirms it's truly optional
        ParseResult result = parser.parse(validProgramJson());
        assertThat(result).isInstanceOf(ParseResult.Success.class);
        Program program = ((ParseResult.Success) result).program();
        assertThat(program.getWeeks().get(0).getDays().get(0).getSections().get(0)
                .getExercises().get(0).getModalityType()).isNull();
    }

    @Test
    void parse_ModalityTypePresentOnHypertrophyDay_ReturnsSuccess() {
        String json = validProgramJson().replace(
                "\"prescribed_reps\": \"8-10\"",
                "\"prescribed_reps\": \"8-10\", \"modality_type\": \"Weightlifting\"");
        ParseResult result = parser.parse(json);
        assertThat(result).isInstanceOf(ParseResult.Success.class);
        Program program = ((ParseResult.Success) result).program();
        assertThat(program.getWeeks().get(0).getDays().get(0).getSections().get(0)
                .getExercises().get(0).getModalityType()).isEqualTo(ModalityType.WEIGHTLIFTING);
    }

    @Test
    void parse_WarmUpAndCoolDownEntries_ParsedCorrectly() {
        ParseResult result = parser.parse(validProgramJson());
        assertThat(result).isInstanceOf(ParseResult.Success.class);
        Program program = ((ParseResult.Success) result).program();

        var day = program.getWeeks().get(0).getDays().get(0);

        assertThat(day.getWarmUp()).hasSize(1);
        assertThat(day.getWarmUp().get(0).movement()).isEqualTo("Arm circles");
        assertThat(day.getWarmUp().get(0).instruction()).isEqualTo("10 reps each direction");

        assertThat(day.getCoolDown()).hasSize(1);
        assertThat(day.getCoolDown().get(0).movement()).isEqualTo("Chest stretch");
        assertThat(day.getCoolDown().get(0).instruction()).isEqualTo("Hold 30 seconds");
    }
}
