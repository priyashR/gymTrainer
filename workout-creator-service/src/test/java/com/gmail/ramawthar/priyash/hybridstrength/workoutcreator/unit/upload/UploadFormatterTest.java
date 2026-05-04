package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit.upload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UploadFormatter}.
 * Each test verifies that a known {@link Program} domain object produces the expected JSON structure.
 * Naming convention: format_StateUnderTest_ExpectedBehaviour
 */
class UploadFormatterTest {

    private UploadFormatter formatter;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        formatter = new UploadFormatter();
        mapper = new ObjectMapper();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private JsonNode parse(String json) throws Exception {
        return mapper.readTree(json);
    }

    /** Minimal Hypertrophy exercise with no optional fields. */
    private Exercise minimalExercise() {
        return new Exercise("Bench Press", null, 4, "8-10", null, null, null);
    }

    /** Minimal CrossFit exercise with required modality_type. */
    private Exercise crossFitExercise(ModalityType modalityType) {
        return new Exercise("Clean and Jerk", modalityType, 3, "5", "70% 1RM", 120, "Focus on form");
    }

    private Section minimalSection(List<Exercise> exercises) {
        return new Section("Tier 1: Compound", SectionType.STRENGTH, "Sets/Reps", null, exercises);
    }

    private Day minimalDay(int dayNumber, Modality modality, List<Section> sections) {
        return new Day(
                dayNumber, "Monday", "Push", modality,
                List.of(new WarmCoolEntry("Arm circles", "10 reps each direction")),
                sections,
                List.of(new WarmCoolEntry("Chest stretch", "Hold 30 seconds")),
                null
        );
    }

    private Program oneWeekProgram(List<Day> days) {
        Week week = new Week(1, days);
        return new Program("Test Program", 1, "Hypertrophy", List.of("Barbell", "Dumbbells"), List.of(week));
    }

    // ── program_metadata ─────────────────────────────────────────────────────

    @Test
    void format_ValidProgram_OutputsCorrectProgramName() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));

        assertThat(root.path("program_metadata").path("program_name").asText()).isEqualTo("Test Program");
    }

    @Test
    void format_ValidProgram_OutputsCorrectDurationWeeks() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));

        assertThat(root.path("program_metadata").path("duration_weeks").asInt()).isEqualTo(1);
    }

    @Test
    void format_ValidProgram_OutputsCorrectGoal() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));

        assertThat(root.path("program_metadata").path("goal").asText()).isEqualTo("Hypertrophy");
    }

    @Test
    void format_ValidProgram_OutputsVersionAsOneDotZero() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));

        assertThat(root.path("program_metadata").path("version").asText()).isEqualTo("1.0");
    }

    @Test
    void format_ValidProgram_OutputsEquipmentProfile() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));

        JsonNode equipArray = root.path("program_metadata").path("equipment_profile");
        assertThat(equipArray.isArray()).isTrue();
        assertThat(equipArray).hasSize(2);
        assertThat(equipArray.get(0).asText()).isEqualTo("Barbell");
        assertThat(equipArray.get(1).asText()).isEqualTo("Dumbbells");
    }

    // ── program_structure ────────────────────────────────────────────────────

    @Test
    void format_OneWeekProgram_OutputsOneWeekInStructure() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));

        assertThat(root.path("program_structure").isArray()).isTrue();
        assertThat(root.path("program_structure")).hasSize(1);
    }

    @Test
    void format_FourWeekProgram_OutputsFourWeeksInStructure() throws Exception {
        List<Week> weeks = List.of(
                new Week(1, List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise())))))),
                new Week(2, List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise())))))),
                new Week(3, List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise())))))),
                new Week(4, List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))))
        );
        Program program = new Program("4-Week Program", 4, "Strength", List.of("Barbell"), weeks);

        JsonNode root = parse(formatter.format(program));

        assertThat(root.path("program_structure")).hasSize(4);
        assertThat(root.path("program_structure").get(3).path("week_number").asInt()).isEqualTo(4);
    }

    @Test
    void format_ValidProgram_OutputsCorrectWeekNumber() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));

        assertThat(root.path("program_structure").get(0).path("week_number").asInt()).isEqualTo(1);
    }

    // ── day fields ───────────────────────────────────────────────────────────

    @Test
    void format_ValidProgram_OutputsCorrectDayFields() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));
        JsonNode day = root.path("program_structure").get(0).path("days").get(0);

        assertThat(day.path("day_number").asInt()).isEqualTo(1);
        assertThat(day.path("day_label").asText()).isEqualTo("Monday");
        assertThat(day.path("focus_area").asText()).isEqualTo("Push");
    }

    @Test
    void format_HypertrophyDay_OutputsModalityAsHypertrophy() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));
        JsonNode day = root.path("program_structure").get(0).path("days").get(0);

        assertThat(day.path("modality").asText()).isEqualTo("Hypertrophy");
    }

    @Test
    void format_CrossFitDay_OutputsModalityAsCrossFit() throws Exception {
        Exercise ex = crossFitExercise(ModalityType.WEIGHTLIFTING);
        Day crossFitDay = new Day(1, "Monday", "Metcon", Modality.CROSSFIT,
                List.of(), List.of(minimalSection(List.of(ex))), List.of(), null);
        Program program = oneWeekProgram(List.of(crossFitDay));

        JsonNode root = parse(formatter.format(program));
        JsonNode day = root.path("program_structure").get(0).path("days").get(0);

        assertThat(day.path("modality").asText()).isEqualTo("CrossFit");
    }

    @Test
    void format_DayWithMethodologySource_OutputsMethodologySource() throws Exception {
        Day day = new Day(1, "Monday", "Push", Modality.HYPERTROPHY,
                List.of(), List.of(minimalSection(List.of(minimalExercise()))), List.of(),
                "Inspired by: Renaissance Periodization");
        Program program = oneWeekProgram(List.of(day));

        JsonNode root = parse(formatter.format(program));
        JsonNode dayNode = root.path("program_structure").get(0).path("days").get(0);

        assertThat(dayNode.path("methodology_source").asText()).isEqualTo("Inspired by: Renaissance Periodization");
    }

    @Test
    void format_DayWithNullMethodologySource_OmitsMethodologySourceField() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));
        JsonNode dayNode = root.path("program_structure").get(0).path("days").get(0);

        assertThat(dayNode.has("methodology_source")).isFalse();
    }

    // ── warm_up / cool_down ──────────────────────────────────────────────────

    @Test
    void format_DayWithWarmUp_OutputsWarmUpEntries() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));
        JsonNode warmUp = root.path("program_structure").get(0).path("days").get(0).path("warm_up");

        assertThat(warmUp.isArray()).isTrue();
        assertThat(warmUp).hasSize(1);
        assertThat(warmUp.get(0).path("movement").asText()).isEqualTo("Arm circles");
        assertThat(warmUp.get(0).path("instruction").asText()).isEqualTo("10 reps each direction");
    }

    @Test
    void format_DayWithCoolDown_OutputsCoolDownEntries() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));
        JsonNode coolDown = root.path("program_structure").get(0).path("days").get(0).path("cool_down");

        assertThat(coolDown.isArray()).isTrue();
        assertThat(coolDown).hasSize(1);
        assertThat(coolDown.get(0).path("movement").asText()).isEqualTo("Chest stretch");
        assertThat(coolDown.get(0).path("instruction").asText()).isEqualTo("Hold 30 seconds");
    }

    // ── blocks ───────────────────────────────────────────────────────────────

    @Test
    void format_ValidProgram_OutputsBlockTypeAndFormat() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));
        JsonNode block = root.path("program_structure").get(0).path("days").get(0).path("blocks").get(0);

        assertThat(block.path("block_type").asText()).isEqualTo("Tier 1: Compound");
        assertThat(block.path("format").asText()).isEqualTo("Sets/Reps");
    }

    @Test
    void format_BlockWithTimeCap_OutputsTimeCap() throws Exception {
        Section timedSection = new Section("Metcon", SectionType.AMRAP, "AMRAP", 20, List.of(minimalExercise()));
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(timedSection))));

        JsonNode root = parse(formatter.format(program));
        JsonNode block = root.path("program_structure").get(0).path("days").get(0).path("blocks").get(0);

        assertThat(block.path("time_cap_minutes").asInt()).isEqualTo(20);
    }

    @Test
    void format_BlockWithNullTimeCap_OmitsTimeCapField() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));
        JsonNode block = root.path("program_structure").get(0).path("days").get(0).path("blocks").get(0);

        assertThat(block.has("time_cap_minutes")).isFalse();
    }

    // ── movements ────────────────────────────────────────────────────────────

    @Test
    void format_ValidProgram_OutputsRequiredMovementFields() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));
        JsonNode movement = root.path("program_structure").get(0).path("days").get(0)
                .path("blocks").get(0).path("movements").get(0);

        assertThat(movement.path("exercise_name").asText()).isEqualTo("Bench Press");
        assertThat(movement.path("prescribed_sets").asInt()).isEqualTo(4);
        assertThat(movement.path("prescribed_reps").asText()).isEqualTo("8-10");
    }

    @Test
    void format_MovementWithAllOptionalFields_OutputsAllFields() throws Exception {
        Exercise ex = new Exercise("Squat", null, 5, "5", "80% 1RM", 180, "Pause at bottom");
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(ex))))));

        JsonNode root = parse(formatter.format(program));
        JsonNode movement = root.path("program_structure").get(0).path("days").get(0)
                .path("blocks").get(0).path("movements").get(0);

        assertThat(movement.path("prescribed_weight").asText()).isEqualTo("80% 1RM");
        assertThat(movement.path("rest_interval_seconds").asInt()).isEqualTo(180);
        assertThat(movement.path("notes").asText()).isEqualTo("Pause at bottom");
    }

    @Test
    void format_MovementWithNullOptionalFields_OmitsOptionalFields() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));
        JsonNode movement = root.path("program_structure").get(0).path("days").get(0)
                .path("blocks").get(0).path("movements").get(0);

        assertThat(movement.has("prescribed_weight")).isFalse();
        assertThat(movement.has("rest_interval_seconds")).isFalse();
        assertThat(movement.has("notes")).isFalse();
        assertThat(movement.has("modality_type")).isFalse();
    }

    @Test
    void format_CrossFitMovementWithEngineModalityType_OutputsEngineString() throws Exception {
        Exercise ex = crossFitExercise(ModalityType.ENGINE);
        Day crossFitDay = new Day(1, "Monday", "Metcon", Modality.CROSSFIT,
                List.of(), List.of(minimalSection(List.of(ex))), List.of(), null);
        Program program = oneWeekProgram(List.of(crossFitDay));

        JsonNode root = parse(formatter.format(program));
        JsonNode movement = root.path("program_structure").get(0).path("days").get(0)
                .path("blocks").get(0).path("movements").get(0);

        assertThat(movement.path("modality_type").asText()).isEqualTo("Engine");
    }

    @Test
    void format_CrossFitMovementWithGymnasticsModalityType_OutputsGymnasticsString() throws Exception {
        Exercise ex = crossFitExercise(ModalityType.GYMNASTICS);
        Day crossFitDay = new Day(1, "Monday", "Metcon", Modality.CROSSFIT,
                List.of(), List.of(minimalSection(List.of(ex))), List.of(), null);
        Program program = oneWeekProgram(List.of(crossFitDay));

        JsonNode root = parse(formatter.format(program));
        JsonNode movement = root.path("program_structure").get(0).path("days").get(0)
                .path("blocks").get(0).path("movements").get(0);

        assertThat(movement.path("modality_type").asText()).isEqualTo("Gymnastics");
    }

    @Test
    void format_CrossFitMovementWithWeightliftingModalityType_OutputsWeightliftingString() throws Exception {
        Exercise ex = crossFitExercise(ModalityType.WEIGHTLIFTING);
        Day crossFitDay = new Day(1, "Monday", "Metcon", Modality.CROSSFIT,
                List.of(), List.of(minimalSection(List.of(ex))), List.of(), null);
        Program program = oneWeekProgram(List.of(crossFitDay));

        JsonNode root = parse(formatter.format(program));
        JsonNode movement = root.path("program_structure").get(0).path("days").get(0)
                .path("blocks").get(0).path("movements").get(0);

        assertThat(movement.path("modality_type").asText()).isEqualTo("Weightlifting");
    }

    // ── output validity ──────────────────────────────────────────────────────

    @Test
    void format_ValidProgram_OutputIsValidJson() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        String json = formatter.format(program);

        // Should not throw
        JsonNode root = parse(json);
        assertThat(root).isNotNull();
        assertThat(root.isObject()).isTrue();
    }

    @Test
    void format_ValidProgram_OutputContainsBothTopLevelKeys() throws Exception {
        Program program = oneWeekProgram(List.of(minimalDay(1, Modality.HYPERTROPHY, List.of(minimalSection(List.of(minimalExercise()))))));

        JsonNode root = parse(formatter.format(program));

        assertThat(root.has("program_metadata")).isTrue();
        assertThat(root.has("program_structure")).isTrue();
    }
}
