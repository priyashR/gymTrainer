package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProgramEntityMapper}.
 * Naming convention: MethodName_StateUnderTest_ExpectedBehaviour
 */
class ProgramEntityMapperTest {

    private static final UUID PROGRAM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-16T12:00:00Z");

    // ── helpers ───────────────────────────────────────────────────────────────

    private ProgramJpaEntity minimalProgramEntity() {
        ProgramJpaEntity entity = new ProgramJpaEntity();
        entity.setId(PROGRAM_ID);
        entity.setName("Test Program");
        entity.setDurationWeeks(1);
        entity.setGoal("Build strength");
        entity.setEquipmentProfile("Barbell,Dumbbells");
        entity.setOwnerUserId("user-123");
        entity.setContentSource(ContentSource.UPLOADED);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        entity.setWeeks(new ArrayList<>());
        return entity;
    }

    private ProgramJpaEntity fullProgramEntity() {
        ProgramJpaEntity program = minimalProgramEntity();

        WeekJpaEntity week = new WeekJpaEntity();
        week.setId(UUID.randomUUID());
        week.setWeekNumber(1);
        week.setProgram(program);

        DayJpaEntity day = new DayJpaEntity();
        day.setId(UUID.randomUUID());
        day.setDayNumber(1);
        day.setLabel("Push Day");
        day.setFocusArea("Push");
        day.setModality(Modality.HYPERTROPHY);
        day.setMethodologySource(null);
        day.setWeek(week);

        SectionJpaEntity section = new SectionJpaEntity();
        section.setId(UUID.randomUUID());
        section.setName("Tier 1: Compound");
        section.setSectionType(SectionType.STRENGTH);
        section.setFormat("Sets/Reps");
        section.setTimeCap(null);
        section.setSortOrder(0);
        section.setDay(day);

        ExerciseJpaEntity exercise = new ExerciseJpaEntity();
        exercise.setId(UUID.randomUUID());
        exercise.setName("Bench Press");
        exercise.setModalityType(null);
        exercise.setSets(4);
        exercise.setReps("6-8");
        exercise.setWeight("80kg");
        exercise.setRestSeconds(120);
        exercise.setNotes("Control the eccentric");
        exercise.setSortOrder(0);
        exercise.setSection(section);

        section.setExercises(new ArrayList<>(List.of(exercise)));
        day.setSections(new ArrayList<>(List.of(section)));

        WarmCoolEntryJpaEntity warmUp = new WarmCoolEntryJpaEntity();
        warmUp.setId(UUID.randomUUID());
        warmUp.setEntryType("WARM_UP");
        warmUp.setMovement("Arm Circles");
        warmUp.setInstruction("30 seconds each direction");
        warmUp.setSortOrder(0);
        warmUp.setDay(day);

        WarmCoolEntryJpaEntity coolDown = new WarmCoolEntryJpaEntity();
        coolDown.setId(UUID.randomUUID());
        coolDown.setEntryType("COOL_DOWN");
        coolDown.setMovement("Chest Stretch");
        coolDown.setInstruction("30 seconds each side");
        coolDown.setSortOrder(0);
        coolDown.setDay(day);

        day.setWarmCoolEntries(new ArrayList<>(List.of(warmUp, coolDown)));

        week.setDays(new ArrayList<>(List.of(day)));
        program.setWeeks(new ArrayList<>(List.of(week)));

        return program;
    }

    // ── toProgram ─────────────────────────────────────────────────────────────

    @Nested
    class ToProgram {

        @Test
        void toProgram_EmptyWeeks_ReturnsProgramWithEmptyWeeksList() {
            ProgramJpaEntity entity = minimalProgramEntity();

            Program result = ProgramEntityMapper.toProgram(entity);

            assertThat(result.getWeeks()).isEmpty();
            assertThat(result.getName()).isEqualTo("Test Program");
            assertThat(result.getDurationWeeks()).isEqualTo(1);
            assertThat(result.getGoal()).isEqualTo("Build strength");
        }

        @Test
        void toProgram_FullEntity_MapsAllFieldsCorrectly() {
            ProgramJpaEntity entity = fullProgramEntity();

            Program result = ProgramEntityMapper.toProgram(entity);

            assertThat(result.getName()).isEqualTo("Test Program");
            assertThat(result.getDurationWeeks()).isEqualTo(1);
            assertThat(result.getGoal()).isEqualTo("Build strength");
            assertThat(result.getEquipmentProfile()).containsExactly("Barbell", "Dumbbells");
            assertThat(result.getWeeks()).hasSize(1);

            Week week = result.getWeeks().get(0);
            assertThat(week.getWeekNumber()).isEqualTo(1);
            assertThat(week.getDays()).hasSize(1);

            Day day = week.getDays().get(0);
            assertThat(day.getDayNumber()).isEqualTo(1);
            assertThat(day.getLabel()).isEqualTo("Push Day");
            assertThat(day.getFocusArea()).isEqualTo("Push");
            assertThat(day.getModality()).isEqualTo(Modality.HYPERTROPHY);
            assertThat(day.getMethodologySource()).isNull();
            assertThat(day.getWarmUp()).hasSize(1);
            assertThat(day.getCoolDown()).hasSize(1);
            assertThat(day.getSections()).hasSize(1);

            Section section = day.getSections().get(0);
            assertThat(section.getName()).isEqualTo("Tier 1: Compound");
            assertThat(section.getSectionType()).isEqualTo(SectionType.STRENGTH);
            assertThat(section.getFormat()).isEqualTo("Sets/Reps");
            assertThat(section.getTimeCap()).isNull();
            assertThat(section.getExercises()).hasSize(1);

            Exercise exercise = section.getExercises().get(0);
            assertThat(exercise.getName()).isEqualTo("Bench Press");
            assertThat(exercise.getModalityType()).isNull();
            assertThat(exercise.getSets()).isEqualTo(4);
            assertThat(exercise.getReps()).isEqualTo("6-8");
            assertThat(exercise.getWeight()).isEqualTo("80kg");
            assertThat(exercise.getRestSeconds()).isEqualTo(120);
            assertThat(exercise.getNotes()).isEqualTo("Control the eccentric");
        }

        @Test
        void toProgram_NullEquipmentProfile_ReturnsEmptyList() {
            ProgramJpaEntity entity = minimalProgramEntity();
            entity.setEquipmentProfile(null);

            Program result = ProgramEntityMapper.toProgram(entity);

            assertThat(result.getEquipmentProfile()).isEmpty();
        }

        @Test
        void toProgram_BlankEquipmentProfile_ReturnsEmptyList() {
            ProgramJpaEntity entity = minimalProgramEntity();
            entity.setEquipmentProfile("   ");

            Program result = ProgramEntityMapper.toProgram(entity);

            assertThat(result.getEquipmentProfile()).isEmpty();
        }

        @Test
        void toProgram_WarmUpAndCoolDownEntries_MappedCorrectly() {
            ProgramJpaEntity entity = fullProgramEntity();

            Program result = ProgramEntityMapper.toProgram(entity);

            Day day = result.getWeeks().get(0).getDays().get(0);
            assertThat(day.getWarmUp()).hasSize(1);
            assertThat(day.getWarmUp().get(0).movement()).isEqualTo("Arm Circles");
            assertThat(day.getWarmUp().get(0).instruction()).isEqualTo("30 seconds each direction");
            assertThat(day.getCoolDown()).hasSize(1);
            assertThat(day.getCoolDown().get(0).movement()).isEqualTo("Chest Stretch");
            assertThat(day.getCoolDown().get(0).instruction()).isEqualTo("30 seconds each side");
        }

        @Test
        void toProgram_ExerciseWithNullOptionalFields_MapsNullsCorrectly() {
            ProgramJpaEntity entity = fullProgramEntity();
            ExerciseJpaEntity exercise = entity.getWeeks().get(0).getDays().get(0)
                    .getSections().get(0).getExercises().get(0);
            exercise.setWeight(null);
            exercise.setRestSeconds(null);
            exercise.setNotes(null);
            exercise.setModalityType(null);

            Program result = ProgramEntityMapper.toProgram(entity);

            Exercise mapped = result.getWeeks().get(0).getDays().get(0)
                    .getSections().get(0).getExercises().get(0);
            assertThat(mapped.getWeight()).isNull();
            assertThat(mapped.getRestSeconds()).isNull();
            assertThat(mapped.getNotes()).isNull();
            assertThat(mapped.getModalityType()).isNull();
        }

        @Test
        void toProgram_DayWithEmptySections_ReturnsDayWithEmptySectionsList() {
            ProgramJpaEntity entity = minimalProgramEntity();
            WeekJpaEntity week = new WeekJpaEntity();
            week.setId(UUID.randomUUID());
            week.setWeekNumber(1);
            week.setProgram(entity);

            DayJpaEntity day = new DayJpaEntity();
            day.setId(UUID.randomUUID());
            day.setDayNumber(1);
            day.setLabel("Rest Day");
            day.setFocusArea("Recovery");
            day.setModality(Modality.HYPERTROPHY);
            day.setWeek(week);
            day.setSections(new ArrayList<>());
            day.setWarmCoolEntries(new ArrayList<>());

            week.setDays(new ArrayList<>(List.of(day)));
            entity.setWeeks(new ArrayList<>(List.of(week)));

            Program result = ProgramEntityMapper.toProgram(entity);

            Day mappedDay = result.getWeeks().get(0).getDays().get(0);
            assertThat(mappedDay.getSections()).isEmpty();
            assertThat(mappedDay.getWarmUp()).isEmpty();
            assertThat(mappedDay.getCoolDown()).isEmpty();
        }
    }

    // ── toVaultProgram ────────────────────────────────────────────────────────

    @Nested
    class ToVaultProgram {

        @Test
        void toVaultProgram_FullEntity_MapsMetadataCorrectly() {
            ProgramJpaEntity entity = fullProgramEntity();

            VaultProgram result = ProgramEntityMapper.toVaultProgram(entity);

            assertThat(result.id()).isEqualTo(PROGRAM_ID);
            assertThat(result.ownerUserId()).isEqualTo("user-123");
            assertThat(result.contentSource()).isEqualTo(ContentSource.UPLOADED);
            assertThat(result.createdAt()).isEqualTo(CREATED_AT);
            assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
            assertThat(result.program()).isNotNull();
            assertThat(result.program().getName()).isEqualTo("Test Program");
        }

        @Test
        void toVaultProgram_EmptyWeeks_ProgramHasEmptyWeeks() {
            ProgramJpaEntity entity = minimalProgramEntity();

            VaultProgram result = ProgramEntityMapper.toVaultProgram(entity);

            assertThat(result.program().getWeeks()).isEmpty();
        }
    }

    // ── toVaultItem ───────────────────────────────────────────────────────────

    @Nested
    class ToVaultItem {

        @Test
        void toVaultItem_FullEntity_MapsAllSummaryFields() {
            ProgramJpaEntity entity = fullProgramEntity();

            VaultItem result = ProgramEntityMapper.toVaultItem(entity);

            assertThat(result.id()).isEqualTo(PROGRAM_ID);
            assertThat(result.name()).isEqualTo("Test Program");
            assertThat(result.goal()).isEqualTo("Build strength");
            assertThat(result.durationWeeks()).isEqualTo(1);
            assertThat(result.equipmentProfile()).containsExactly("Barbell", "Dumbbells");
            assertThat(result.contentSource()).isEqualTo(ContentSource.UPLOADED);
            assertThat(result.createdAt()).isEqualTo(CREATED_AT);
            assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
        }

        @Test
        void toVaultItem_NullEquipmentProfile_ReturnsEmptyList() {
            ProgramJpaEntity entity = minimalProgramEntity();
            entity.setEquipmentProfile(null);

            VaultItem result = ProgramEntityMapper.toVaultItem(entity);

            assertThat(result.equipmentProfile()).isEmpty();
        }

        @Test
        void toVaultItem_BlankEquipmentProfile_ReturnsEmptyList() {
            ProgramJpaEntity entity = minimalProgramEntity();
            entity.setEquipmentProfile("");

            VaultItem result = ProgramEntityMapper.toVaultItem(entity);

            assertThat(result.equipmentProfile()).isEmpty();
        }
    }

    // ── toEntity ──────────────────────────────────────────────────────────────

    @Nested
    class ToEntity {

        @Test
        void toEntity_MinimalProgram_CreatesEntityWithCorrectFields() {
            Program program = new Program("My Program", 1, "Get strong",
                    List.of("Barbell", "Pull-up Bar"), List.of());

            ProgramJpaEntity result = ProgramEntityMapper.toEntity(program);

            assertThat(result.getId()).isNotNull();
            assertThat(result.getName()).isEqualTo("My Program");
            assertThat(result.getDurationWeeks()).isEqualTo(1);
            assertThat(result.getGoal()).isEqualTo("Get strong");
            assertThat(result.getEquipmentProfile()).isEqualTo("Barbell,Pull-up Bar");
            assertThat(result.getWeeks()).isEmpty();
        }

        @Test
        void toEntity_ProgramWithWeeks_CreatesWeekEntitiesWithBackReference() {
            Exercise exercise = new Exercise("Squat", null, 5, "5", "100kg", 180, null);
            Section section = new Section("Main Lift", SectionType.STRENGTH, "Sets/Reps", null, List.of(exercise));
            Day day = new Day(1, "Leg Day", "Legs", Modality.HYPERTROPHY,
                    List.of(), List.of(section), List.of(), null);
            Week week = new Week(1, List.of(day));
            Program program = new Program("Leg Program", 1, "Build legs",
                    List.of("Barbell"), List.of(week));

            ProgramJpaEntity result = ProgramEntityMapper.toEntity(program);

            assertThat(result.getWeeks()).hasSize(1);
            WeekJpaEntity weekEntity = result.getWeeks().get(0);
            assertThat(weekEntity.getWeekNumber()).isEqualTo(1);
            assertThat(weekEntity.getProgram()).isSameAs(result);
            assertThat(weekEntity.getDays()).hasSize(1);

            DayJpaEntity dayEntity = weekEntity.getDays().get(0);
            assertThat(dayEntity.getDayNumber()).isEqualTo(1);
            assertThat(dayEntity.getLabel()).isEqualTo("Leg Day");
            assertThat(dayEntity.getFocusArea()).isEqualTo("Legs");
            assertThat(dayEntity.getModality()).isEqualTo(Modality.HYPERTROPHY);
            assertThat(dayEntity.getWeek()).isSameAs(weekEntity);
        }

        @Test
        void toEntity_ProgramWithWarmUpAndCoolDown_CreatesWarmCoolEntities() {
            WarmCoolEntry warmUp = new WarmCoolEntry("Jumping Jacks", "2 minutes");
            WarmCoolEntry coolDown = new WarmCoolEntry("Static Stretch", "5 minutes");
            Section section = new Section("Block", SectionType.STRENGTH, "Sets/Reps", null,
                    List.of(new Exercise("Deadlift", null, 3, "5", null, null, null)));
            Day day = new Day(1, "Pull Day", "Pull", Modality.HYPERTROPHY,
                    List.of(warmUp), List.of(section), List.of(coolDown), null);
            Week week = new Week(1, List.of(day));
            Program program = new Program("Pull Program", 1, "Build back",
                    List.of("Barbell"), List.of(week));

            ProgramJpaEntity result = ProgramEntityMapper.toEntity(program);

            DayJpaEntity dayEntity = result.getWeeks().get(0).getDays().get(0);
            assertThat(dayEntity.getWarmCoolEntries()).hasSize(2);

            WarmCoolEntryJpaEntity warmUpEntity = dayEntity.getWarmCoolEntries().stream()
                    .filter(e -> "WARM_UP".equals(e.getEntryType()))
                    .findFirst().orElseThrow();
            assertThat(warmUpEntity.getMovement()).isEqualTo("Jumping Jacks");
            assertThat(warmUpEntity.getInstruction()).isEqualTo("2 minutes");

            WarmCoolEntryJpaEntity coolDownEntity = dayEntity.getWarmCoolEntries().stream()
                    .filter(e -> "COOL_DOWN".equals(e.getEntryType()))
                    .findFirst().orElseThrow();
            assertThat(coolDownEntity.getMovement()).isEqualTo("Static Stretch");
            assertThat(coolDownEntity.getInstruction()).isEqualTo("5 minutes");
        }

        @Test
        void toEntity_EmptyEquipmentProfile_StoresEmptyString() {
            Program program = new Program("Program", 1, "Goal", List.of(), List.of());

            ProgramJpaEntity result = ProgramEntityMapper.toEntity(program);

            assertThat(result.getEquipmentProfile()).isEmpty();
        }

        @Test
        void toEntity_ExerciseWithAllNullOptionalFields_CreatesEntityWithNulls() {
            Exercise exercise = new Exercise("Push-up", null, 3, "10", null, null, null);
            Section section = new Section("Block", SectionType.STRENGTH, "Sets/Reps", null, List.of(exercise));
            Day day = new Day(1, "Day 1", "Push", Modality.HYPERTROPHY,
                    List.of(), List.of(section), List.of(), null);
            Week week = new Week(1, List.of(day));
            Program program = new Program("Bodyweight", 1, "Fitness",
                    List.of("None"), List.of(week));

            ProgramJpaEntity result = ProgramEntityMapper.toEntity(program);

            ExerciseJpaEntity exerciseEntity = result.getWeeks().get(0).getDays().get(0)
                    .getSections().get(0).getExercises().get(0);
            assertThat(exerciseEntity.getName()).isEqualTo("Push-up");
            assertThat(exerciseEntity.getModalityType()).isNull();
            assertThat(exerciseEntity.getWeight()).isNull();
            assertThat(exerciseEntity.getRestSeconds()).isNull();
            assertThat(exerciseEntity.getNotes()).isNull();
        }

        @Test
        void toEntity_SectionWithTimeCap_MapsTimeCapCorrectly() {
            Exercise exercise = new Exercise("Burpees", ModalityType.ENGINE, 1, "Max", null, null, null);
            Section section = new Section("Metcon", SectionType.AMRAP, "AMRAP", 12, List.of(exercise));
            Day day = new Day(1, "WOD", "Metcon", Modality.CROSSFIT,
                    List.of(), List.of(section), List.of(), null);
            Week week = new Week(1, List.of(day));
            Program program = new Program("CrossFit WOD", 1, "Conditioning",
                    List.of("None"), List.of(week));

            ProgramJpaEntity result = ProgramEntityMapper.toEntity(program);

            SectionJpaEntity sectionEntity = result.getWeeks().get(0).getDays().get(0)
                    .getSections().get(0);
            assertThat(sectionEntity.getTimeCap()).isEqualTo(12);
            assertThat(sectionEntity.getSectionType()).isEqualTo(SectionType.AMRAP);
            assertThat(sectionEntity.getFormat()).isEqualTo("AMRAP");
        }
    }

    // ── rebuildEntityContent ──────────────────────────────────────────────────

    @Nested
    class RebuildEntityContent {

        @Test
        void rebuildEntityContent_NewProgram_ReplacesWeeksCompletely() {
            ProgramJpaEntity entity = fullProgramEntity();
            assertThat(entity.getWeeks()).hasSize(1);

            // New program with different structure
            Exercise newExercise = new Exercise("Squat", null, 5, "5", "120kg", 180, null);
            Section newSection = new Section("Legs", SectionType.STRENGTH, "Sets/Reps", null, List.of(newExercise));
            Day newDay = new Day(1, "Leg Day", "Legs", Modality.HYPERTROPHY,
                    List.of(), List.of(newSection), List.of(), null);
            Week newWeek1 = new Week(1, List.of(newDay));
            Week newWeek2 = new Week(2, List.of(newDay));
            Program newProgram = new Program("New Program", 2, "New Goal",
                    List.of("Squat Rack"), List.of(newWeek1, newWeek2));

            ProgramEntityMapper.rebuildEntityContent(entity, newProgram);

            assertThat(entity.getName()).isEqualTo("New Program");
            assertThat(entity.getDurationWeeks()).isEqualTo(2);
            assertThat(entity.getGoal()).isEqualTo("New Goal");
            assertThat(entity.getEquipmentProfile()).isEqualTo("Squat Rack");
            assertThat(entity.getWeeks()).hasSize(2);
        }

        @Test
        void rebuildEntityContent_PreservesEntityId() {
            ProgramJpaEntity entity = fullProgramEntity();
            UUID originalId = entity.getId();

            Program newProgram = new Program("New", 1, "Goal", List.of("Bar"), List.of());

            ProgramEntityMapper.rebuildEntityContent(entity, newProgram);

            assertThat(entity.getId()).isEqualTo(originalId);
        }

        @Test
        void rebuildEntityContent_PreservesOwnerAndContentSource() {
            ProgramJpaEntity entity = fullProgramEntity();
            String originalOwner = entity.getOwnerUserId();
            ContentSource originalSource = entity.getContentSource();

            Program newProgram = new Program("New", 1, "Goal", List.of("Bar"), List.of());

            ProgramEntityMapper.rebuildEntityContent(entity, newProgram);

            assertThat(entity.getOwnerUserId()).isEqualTo(originalOwner);
            assertThat(entity.getContentSource()).isEqualTo(originalSource);
        }
    }

    // ── round-trip ────────────────────────────────────────────────────────────

    @Nested
    class RoundTrip {

        @Test
        void roundTrip_ProgramToEntityAndBack_ProducesEquivalentProgram() {
            Exercise exercise = new Exercise("Bench Press", ModalityType.WEIGHTLIFTING, 4, "6-8",
                    "80kg", 120, "Pause at bottom");
            Section section = new Section("Tier 1", SectionType.STRENGTH, "Sets/Reps", null, List.of(exercise));
            WarmCoolEntry warmUp = new WarmCoolEntry("Arm Circles", "30 seconds");
            WarmCoolEntry coolDown = new WarmCoolEntry("Chest Stretch", "60 seconds");
            Day day = new Day(1, "Push Day", "Push", Modality.HYPERTROPHY,
                    List.of(warmUp), List.of(section), List.of(coolDown), "RP Hypertrophy");
            Week week = new Week(1, List.of(day));
            Program original = new Program("Strength Program", 1, "Build strength",
                    List.of("Barbell", "Bench"), List.of(week));

            ProgramJpaEntity entity = ProgramEntityMapper.toEntity(original);
            Program roundTripped = ProgramEntityMapper.toProgram(entity);

            assertThat(roundTripped).isEqualTo(original);
        }

        @Test
        void roundTrip_EmptyProgram_ProducesEquivalentProgram() {
            Program original = new Program("Empty", 1, "Nothing", List.of("None"), List.of());

            ProgramJpaEntity entity = ProgramEntityMapper.toEntity(original);
            Program roundTripped = ProgramEntityMapper.toProgram(entity);

            assertThat(roundTripped).isEqualTo(original);
        }
    }
}
