package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadedProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.ports.outbound.UploadProgramRepository;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.DayJpaEntity;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ExerciseJpaEntity;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ProgramJpaEntity;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ProgramSpringDataRepository;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.SectionJpaEntity;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.WarmCoolEntryJpaEntity;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.WeekJpaEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Outbound adapter: persists an {@link UploadedProgram} using the existing JPA entity
 * infrastructure from the {@code vault} package.
 *
 * <p>Always sets {@code content_source = UPLOADED} on every save, regardless of what
 * the domain object carries — this is an invariant of the upload flow.
 *
 * <p>After persistence the adapter reconstructs an {@link UploadedProgram} from the
 * saved entity so the application service can build the full response without touching
 * JPA types.
 */
@Repository
public class JpaUploadProgramRepository implements UploadProgramRepository {

    private final ProgramSpringDataRepository programRepo;

    public JpaUploadProgramRepository(ProgramSpringDataRepository programRepo) {
        this.programRepo = programRepo;
    }

    @Override
    public UploadedProgram save(UploadedProgram uploadedProgram) {
        ProgramJpaEntity entity = toEntity(uploadedProgram);
        ProgramJpaEntity saved = programRepo.save(entity);
        return toDomain(saved);
    }

    // -------------------------------------------------------------------------
    // Domain → Entity
    // -------------------------------------------------------------------------

    private ProgramJpaEntity toEntity(UploadedProgram uploadedProgram) {
        Program program = uploadedProgram.program();
        Instant now = Instant.now();

        ProgramJpaEntity entity = new ProgramJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(program.getName());
        entity.setDurationWeeks(program.getDurationWeeks());
        entity.setGoal(program.getGoal());
        entity.setEquipmentProfile(String.join(",", program.getEquipmentProfile()));
        entity.setOwnerUserId(uploadedProgram.ownerUserId());
        entity.setContentSource(ContentSource.UPLOADED); // invariant: always UPLOADED
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        List<WeekJpaEntity> weekEntities = new ArrayList<>();
        for (Week week : program.getWeeks()) {
            WeekJpaEntity weekEntity = toWeekEntity(week, entity);
            weekEntities.add(weekEntity);
        }
        entity.setWeeks(weekEntities);

        return entity;
    }

    private WeekJpaEntity toWeekEntity(Week week, ProgramJpaEntity programEntity) {
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

    private DayJpaEntity toDayEntity(Day day, WeekJpaEntity weekEntity) {
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

    private SectionJpaEntity toSectionEntity(Section section, int sortOrder, DayJpaEntity dayEntity) {
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

    private ExerciseJpaEntity toExerciseEntity(Exercise exercise, int sortOrder, SectionJpaEntity sectionEntity) {
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

    private WarmCoolEntryJpaEntity toWarmCoolEntity(WarmCoolEntry entry, String entryType,
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
    // Entity → Domain
    // -------------------------------------------------------------------------

    private UploadedProgram toDomain(ProgramJpaEntity entity) {
        List<Week> weeks = entity.getWeeks().stream()
                .map(this::toWeekDomain)
                .toList();

        List<String> equipmentProfile = List.of(entity.getEquipmentProfile().split(","));

        Program program = new Program(
                entity.getName(),
                entity.getDurationWeeks(),
                entity.getGoal(),
                equipmentProfile,
                weeks
        );

        return new UploadedProgram(
                entity.getId().toString(),
                program,
                entity.getOwnerUserId(),
                entity.getContentSource(),
                entity.getCreatedAt()
        );
    }

    private Week toWeekDomain(WeekJpaEntity entity) {
        List<Day> days = entity.getDays().stream()
                .map(this::toDayDomain)
                .toList();
        return new Week(entity.getWeekNumber(), days);
    }

    private Day toDayDomain(DayJpaEntity entity) {
        List<Section> sections = entity.getSections().stream()
                .map(this::toSectionDomain)
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

    private Section toSectionDomain(SectionJpaEntity entity) {
        List<Exercise> exercises = entity.getExercises().stream()
                .map(this::toExerciseDomain)
                .toList();
        return new Section(
                entity.getName(),
                entity.getSectionType(),
                entity.getFormat(),
                entity.getTimeCap(),
                exercises
        );
    }

    private Exercise toExerciseDomain(ExerciseJpaEntity entity) {
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
}
