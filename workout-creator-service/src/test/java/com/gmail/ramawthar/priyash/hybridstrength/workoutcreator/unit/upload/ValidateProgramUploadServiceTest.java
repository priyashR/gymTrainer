package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit.upload;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.inbound.dto.ValidateUploadResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.application.ValidateProgramUploadService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ValidateProgramUploadService}.
 * No mocks — the service is pure delegation to {@link UploadParser}, so both are instantiated directly.
 * Naming convention: MethodName_StateUnderTest_ExpectedBehaviour
 */
class ValidateProgramUploadServiceTest {

    private ValidateProgramUploadService service;

    @BeforeEach
    void setUp() {
        service = new ValidateProgramUploadService(new UploadParser());
    }

    // ── valid JSON ────────────────────────────────────────────────────────────

    @Test
    void validate_ValidOneWeekProgram_ReturnsValidTrueWithEmptyErrors() {
        String json = validOneWeekProgramJson();

        ValidateUploadResponse response = service.validate(json);

        assertThat(response.valid()).isTrue();
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void validate_ValidFourWeekProgram_ReturnsValidTrueWithEmptyErrors() {
        String json = validFourWeekProgramJson();

        ValidateUploadResponse response = service.validate(json);

        assertThat(response.valid()).isTrue();
        assertThat(response.errors()).isEmpty();
    }

    // ── invalid JSON — schema violations ─────────────────────────────────────

    @Test
    void validate_InvalidDurationWeeks_ReturnsValidFalseWithAtLeastOneError() {
        String json = programJsonWithDurationWeeks(2);

        ValidateUploadResponse response = service.validate(json);

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).isNotEmpty();
    }

    @Test
    void validate_MismatchedProgramStructureLength_ReturnsValidFalseWithAtLeastOneError() {
        // duration_weeks=1 but program_structure has 2 weeks
        String json = programJsonWithStructureLengthMismatch();

        ValidateUploadResponse response = service.validate(json);

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).isNotEmpty();
    }

    @Test
    void validate_WrongVersion_ReturnsValidFalseWithAtLeastOneError() {
        String json = programJsonWithVersion("2.0");

        ValidateUploadResponse response = service.validate(json);

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).isNotEmpty();
    }

    @Test
    void validate_EmptyEquipmentProfile_ReturnsValidFalseWithAtLeastOneError() {
        String json = programJsonWithEmptyEquipmentProfile();

        ValidateUploadResponse response = service.validate(json);

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).isNotEmpty();
    }

    @Test
    void validate_MissingModalityTypeOnCrossFitDay_ReturnsValidFalseWithAtLeastOneError() {
        String json = crossFitProgramJsonWithMissingModalityType();

        ValidateUploadResponse response = service.validate(json);

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).isNotEmpty();
    }

    // ── malformed JSON ────────────────────────────────────────────────────────

    @Test
    void validate_NotValidJson_ReturnsValidFalseWithAtLeastOneError() {
        String json = "this is not json at all";

        ValidateUploadResponse response = service.validate(json);

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).isNotEmpty();
    }

    @Test
    void validate_EmptyString_ReturnsValidFalseWithAtLeastOneError() {
        ValidateUploadResponse response = service.validate("");

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).isNotEmpty();
    }

    // ── nothing is persisted (structural guarantee) ───────────────────────────

    @Test
    void validate_ValidJson_ErrorsListIsEmptyNotNull() {
        ValidateUploadResponse response = service.validate(validOneWeekProgramJson());

        // errors must be an empty list, not null, per the ValidateUploadResponse contract
        assertThat(response.errors()).isNotNull().isEmpty();
    }

    @Test
    void validate_InvalidJson_ErrorsListContainsFieldAndMessage() {
        ValidateUploadResponse response = service.validate(programJsonWithDurationWeeks(3));

        assertThat(response.errors()).isNotEmpty();
        assertThat(response.errors().get(0).field()).isNotBlank();
        assertThat(response.errors().get(0).message()).isNotBlank();
    }

    // ── JSON builders ─────────────────────────────────────────────────────────

    private String validOneWeekProgramJson() {
        return """
                {
                  "program_metadata": {
                    "program_name": "Test Program",
                    "duration_weeks": 1,
                    "goal": "Hypertrophy",
                    "equipment_profile": ["Barbell", "Dumbbell"],
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
                          "cool_down": [{"movement": "Chest stretch", "instruction": "Hold 30s each side"}]
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private String validFourWeekProgramJson() {
        String weekTemplate = """
                {
                  "week_number": %d,
                  "days": [
                    {
                      "day_number": 1,
                      "day_label": "Monday",
                      "focus_area": "Full Body",
                      "modality": "Hypertrophy",
                      "warm_up": [{"movement": "Jog", "instruction": "5 minutes easy"}],
                      "blocks": [
                        {
                          "block_type": "Strength",
                          "format": "Sets/Reps",
                          "movements": [
                            {
                              "exercise_name": "Squat",
                              "prescribed_sets": 3,
                              "prescribed_reps": "5"
                            }
                          ]
                        }
                      ],
                      "cool_down": [{"movement": "Hip flexor stretch", "instruction": "Hold 30s"}]
                    }
                  ]
                }
                """;
        return """
                {
                  "program_metadata": {
                    "program_name": "Four Week Block",
                    "duration_weeks": 4,
                    "goal": "Strength",
                    "equipment_profile": ["Barbell"],
                    "version": "1.0"
                  },
                  "program_structure": [
                    %s, %s, %s, %s
                  ]
                }
                """.formatted(
                weekTemplate.formatted(1),
                weekTemplate.formatted(2),
                weekTemplate.formatted(3),
                weekTemplate.formatted(4)
        );
    }

    private String programJsonWithDurationWeeks(int durationWeeks) {
        return """
                {
                  "program_metadata": {
                    "program_name": "Bad Program",
                    "duration_weeks": %d,
                    "goal": "GPP",
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
                          "focus_area": "Full Body",
                          "modality": "Hypertrophy",
                          "warm_up": [],
                          "blocks": [
                            {
                              "block_type": "Strength",
                              "format": "Sets/Reps",
                              "movements": [
                                {"exercise_name": "Squat", "prescribed_sets": 3, "prescribed_reps": "5"}
                              ]
                            }
                          ],
                          "cool_down": []
                        }
                      ]
                    }
                  ]
                }
                """.formatted(durationWeeks);
    }

    private String programJsonWithStructureLengthMismatch() {
        // duration_weeks=1 but two weeks in program_structure
        return """
                {
                  "program_metadata": {
                    "program_name": "Mismatch Program",
                    "duration_weeks": 1,
                    "goal": "GPP",
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
                          "focus_area": "Full Body",
                          "modality": "Hypertrophy",
                          "warm_up": [],
                          "blocks": [
                            {
                              "block_type": "Strength",
                              "format": "Sets/Reps",
                              "movements": [
                                {"exercise_name": "Squat", "prescribed_sets": 3, "prescribed_reps": "5"}
                              ]
                            }
                          ],
                          "cool_down": []
                        }
                      ]
                    },
                    {
                      "week_number": 2,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Full Body",
                          "modality": "Hypertrophy",
                          "warm_up": [],
                          "blocks": [
                            {
                              "block_type": "Strength",
                              "format": "Sets/Reps",
                              "movements": [
                                {"exercise_name": "Deadlift", "prescribed_sets": 3, "prescribed_reps": "5"}
                              ]
                            }
                          ],
                          "cool_down": []
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private String programJsonWithVersion(String version) {
        return """
                {
                  "program_metadata": {
                    "program_name": "Version Test",
                    "duration_weeks": 1,
                    "goal": "GPP",
                    "equipment_profile": ["Barbell"],
                    "version": "%s"
                  },
                  "program_structure": [
                    {
                      "week_number": 1,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Full Body",
                          "modality": "Hypertrophy",
                          "warm_up": [],
                          "blocks": [
                            {
                              "block_type": "Strength",
                              "format": "Sets/Reps",
                              "movements": [
                                {"exercise_name": "Squat", "prescribed_sets": 3, "prescribed_reps": "5"}
                              ]
                            }
                          ],
                          "cool_down": []
                        }
                      ]
                    }
                  ]
                }
                """.formatted(version);
    }

    private String programJsonWithEmptyEquipmentProfile() {
        return """
                {
                  "program_metadata": {
                    "program_name": "No Equipment",
                    "duration_weeks": 1,
                    "goal": "GPP",
                    "equipment_profile": [],
                    "version": "1.0"
                  },
                  "program_structure": [
                    {
                      "week_number": 1,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Full Body",
                          "modality": "Hypertrophy",
                          "warm_up": [],
                          "blocks": [
                            {
                              "block_type": "Strength",
                              "format": "Sets/Reps",
                              "movements": [
                                {"exercise_name": "Squat", "prescribed_sets": 3, "prescribed_reps": "5"}
                              ]
                            }
                          ],
                          "cool_down": []
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private String crossFitProgramJsonWithMissingModalityType() {
        // CrossFit day with a movement that has no modality_type — should fail validation
        return """
                {
                  "program_metadata": {
                    "program_name": "CrossFit Program",
                    "duration_weeks": 1,
                    "goal": "GPP",
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
                          "focus_area": "Metcon",
                          "modality": "CrossFit",
                          "warm_up": [{"movement": "Row", "instruction": "500m easy"}],
                          "blocks": [
                            {
                              "block_type": "Metcon",
                              "format": "AMRAP",
                              "movements": [
                                {
                                  "exercise_name": "Thruster",
                                  "prescribed_sets": 1,
                                  "prescribed_reps": "21"
                                }
                              ]
                            }
                          ],
                          "cool_down": [{"movement": "Pigeon pose", "instruction": "Hold 60s each side"}]
                        }
                      ]
                    }
                  ]
                }
                """;
    }
}
