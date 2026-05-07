package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Shared utility for mapping between JPA entities and domain objects.
 * Used by both the upload adapter and the vault adapter to avoid duplication.
 *
 * <p>This class is stateless — all methods are static.
 */
public final class ProgramEntityMapper {

    private ProgramEntityMapper() {
        // utility class — no instantiation
    }

    // -------------------------------------------------------------------------
    // Entity → Domain (Program)
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link ProgramJpaEntity} to the shared {@link Program} domain object.
     */
    public static Program toProgram(ProgramJpaEntity entity) {
        List<Week> weeks = entity.getWeeks().stream()
                .map(ProgramEntityMapper::toWeekDomain)
                .toList();

        List<String> equipmentProfile = parseEquipmentProfile(entity.getEquipmentProfile());

        return new Program(
                entity.getName(),
                entity.getDurationWeeks(),
                entity.getGoal(),
                equipmentProfile,
                weeks
        );
    }

    /**
     * Maps a {@link ProgramJpaEntity} to a {@link VaultProgram} domain object
     * containing full program details with metadata.
     */
    public static VaultProgram toVaultProgram(ProgramJpaEntity entity) {
        Program program = toProgram(entity);
        return new VaultProgram(
                entity.getId(),
                program,
                entity.getOwnerUserId(),
                entity.getContentSource(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Maps a {@link ProgramJpaEntity} to a {@link VaultItem} summary representation.
     */
    public static VaultItem toVaultItem(ProgramJpaEntity entity) {
        List<String> equipmentProfile = parseEquipmentProfile(entity.getEquipmentProfile());
        return new VaultItem(
                entity.getId(),
                entity.getName(),
                entity.getGoal(),
                entity.getDurationWeeks(),
                equipmentProfile,
                entity.getContentSource(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // -------------------------------------------------------------------------
    // Domain → Entity (Program)
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link Program} domain object to a new {@link ProgramJpaEntity}.
     * Sets a new random UUID for the entity and all child entities.
     * Does NOT set ownerUserId, contentSource, or timestamps — callers must set those.
     */
    public static ProgramJpaEntity toEntity(Program program) {
        ProgramJpaEntity entity = new ProgramJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(program.getName());
        entity.setDurationWeeks(program.getDurationWeeks());
        entity.setGoal(program.getGoal());
        entity.setEquipmentProfile(String.join(",", program.getEquipmentProfile()));

        List<WeekJpaEntity> weekEntities = new ArrayList<>();
        for (Week week : program.getWeeks()) {
            weekEntities.add(toWeekEntity(week, entity));
        }
        entity.setWeeks(weekEntities);

        return entity;
    }

    /**
     * Rebuilds the child entity tree (weeks, days, sections, exercises, warm-cool entries)
     * on an existing {@link ProgramJpaEntity} from a {@link Program} domain object.
     * Preserves the entity's id, ownerUserId, contentSource, and createdAt.
     * Callers should clear existing weeks before calling this method.
     */
    public static void rebuildEntityContent(ProgramJpaEntity entity, Program program) {
        entity.setName(program.getName());
        entity.setDurationWeeks(program.getDurationWeeks());
        entity.setGoal(program.getGoal());
        entity.setEquipmentProfile(String.join(",", program.getEquipmentProfile()));

        List<WeekJpaEntity> weekEntities = new ArrayList<>();
        for (Week week : program.getWeeks()) {
            weekEntities.add(toWeekEntity(week, entity));
        }
        entity.getWeeks().clear();
        entity.getWeeks().addAll(weekEntities);
    }

    // -------------------------------------------------------------------------
    // Internal helpers — Week
    // -------------------------------------------------------------------------

    private static Week toWeekDomain(WeekJpaEntity entity) {
        List<Day> days = entity.getDays().stream()
                .map(ProgramEntityMapper::toDayDomain)
                .toList();
        return new Week(entity.getWeekNumber(), days);
    }

    private static WeekJpaEntity toWeekEntity(Week week, ProgramJpaEntity programEntity) {
        WeekJpaEntity entity = new WeekJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setWeekNumber(week.getWeekNumber());
        entity.setProgram(programEntity);

        List<DayJpaEntity> dayEntities = new ArrayList<>();
        for (Day day : week.getDays()) {
            dayEntities.add(toDayEntity(day, entity));
        }
        entity.setDays(dayEntities);

        return entity;
    }

    // -------------------------------------------------------------------------
    // Internal helpers — Day
    // -------------------------------------------------------------------------

    private static Day toDayDomain(DayJpaEntity entity) {
        List<Section> sections = entity.getSections().stream()
                .map(ProgramEntityMapper::toSectionDomain)
                .toList();

        List<WarmCoolEntry> warmUp = entity.getWarmCoolEntries().stream()
                .filter(e -> "WARM_UP".equals(e.getEntryType()))
                .map(e -> new WarmCoolEntry(e.getMovement(), e.getInstruction()))
                .toList();

        List<WarmCoolEntry> coolDown = entity.getWarmCoolEntries().stream()
                .filter(e -> "COOL_DOWN".equals(e.getEntryType()))
                .map(e -> new WarmCoolEntry(e.getMovement(), e.getInstruction()))
                .toList();

        return new Day(
                entity.getDayNumber(),
                entity.getLabel(),
                entity.getFocusArea(),
                entity.getModality(),
                warmUp,
                sections,
                coolDown,
                entity.getMethodologySource()
        );
    }

    private static DayJpaEntity toDayEntity(Day day, WeekJpaEntity weekEntity) {
        DayJpaEntity entity = new DayJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setDayNumber(day.getDayNumber());
        entity.setLabel(day.getLabel());
        entity.setFocusArea(day.getFocusArea());
        entity.setModality(day.getModality());
        entity.setMethodologySource(day.getMethodologySource());
        entity.setWeek(weekEntity);

        List<SectionJpaEntity> sectionEntities = new ArrayList<>();
        for (int i = 0; i < day.getSections().size(); i++) {
            sectionEntities.add(toSectionEntity(day.getSections().get(i), i, entity));
        }
        entity.setSections(sectionEntities);

        List<WarmCoolEntryJpaEntity> warmCoolEntities = new ArrayList<>();
        for (int i = 0; i < day.getWarmUp().size(); i++) {
            warmCoolEntities.add(toWarmCoolEntity(day.getWarmUp().get(i), "WARM_UP", i, entity));
        }
        for (int i = 0; i < day.getCoolDown().size(); i++) {
            warmCoolEntities.add(toWarmCoolEntity(day.getCoolDown().get(i), "COOL_DOWN", i, entity));
        }
        entity.setWarmCoolEntries(warmCoolEntities);

        return entity;
    }

    // -------------------------------------------------------------------------
    // Internal helpers — Section
    // -------------------------------------------------------------------------

    private static Section toSectionDomain(SectionJpaEntity entity) {
        List<Exercise> exercises = entity.getExercises().stream()
                .map(ProgramEntityMapper::toExerciseDomain)
                .toList();
        return new Section(
                entity.getName(),
                entity.getSectionType(),
                entity.getFormat(),
                entity.getTimeCap(),
                exercises
        );
    }

    private static SectionJpaEntity toSectionEntity(Section section, int sortOrder, DayJpaEntity dayEntity) {
        SectionJpaEntity entity = new SectionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(section.getName());
        entity.setSectionType(section.getSectionType());
        entity.setFormat(section.getFormat());
        entity.setTimeCap(section.getTimeCap());
        entity.setSortOrder(sortOrder);
        entity.setDay(dayEntity);

        List<ExerciseJpaEntity> exerciseEntities = new ArrayList<>();
        for (int i = 0; i < section.getExercises().size(); i++) {
            exerciseEntities.add(toExerciseEntity(section.getExercises().get(i), i, entity));
        }
        entity.setExercises(exerciseEntities);

        return entity;
    }

    // -------------------------------------------------------------------------
    // Internal helpers — Exercise
    // -------------------------------------------------------------------------

    private static Exercise toExerciseDomain(ExerciseJpaEntity entity) {
        return new Exercise(
                entity.getName(),
                entity.getModalityType(),
                entity.getSets(),
                entity.getReps(),
                entity.getWeight(),
                entity.getRestSeconds(),
                entity.getNotes()
        );
    }

    private static ExerciseJpaEntity toExerciseEntity(Exercise exercise, int sortOrder, SectionJpaEntity sectionEntity) {
        ExerciseJpaEntity entity = new ExerciseJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(exercise.getName());
        entity.setModalityType(exercise.getModalityType());
        entity.setSets(exercise.getSets());
        entity.setReps(exercise.getReps());
        entity.setWeight(exercise.getWeight());
        entity.setRestSeconds(exercise.getRestSeconds());
        entity.setNotes(exercise.getNotes());
        entity.setSortOrder(sortOrder);
        entity.setSection(sectionEntity);
        return entity;
    }

    // -------------------------------------------------------------------------
    // Internal helpers — WarmCoolEntry
    // -------------------------------------------------------------------------

    private static WarmCoolEntryJpaEntity toWarmCoolEntity(WarmCoolEntry entry, String entryType,
                                                            int sortOrder, DayJpaEntity dayEntity) {
        WarmCoolEntryJpaEntity entity = new WarmCoolEntryJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setEntryType(entryType);
        entity.setMovement(entry.movement());
        entity.setInstruction(entry.instruction());
        entity.setSortOrder(sortOrder);
        entity.setDay(dayEntity);
        return entity;
    }

    // -------------------------------------------------------------------------
    // Internal helpers — Equipment Profile
    // -------------------------------------------------------------------------

    private static List<String> parseEquipmentProfile(String equipmentProfile) {
        if (equipmentProfile == null || equipmentProfile.isBlank()) {
            return List.of();
        }
        return Arrays.stream(equipmentProfile.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
