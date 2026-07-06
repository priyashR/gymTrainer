package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DayAssignment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the full program detail endpoint.
 * Includes all nested weeks, days, sections, exercises, and warm-up/cool-down entries.
 * For manual programs, includes dayAssignments instead of weeks.
 */
public record VaultProgramDetailResponse(
        UUID id,
        String name,
        String goal,
        int durationWeeks,
        List<String> equipmentProfile,
        ContentSource contentSource,
        Instant createdAt,
        Instant updatedAt,
        List<WeekResponse> weeks,
        List<DayAssignmentResponse> dayAssignments
) {

    /**
     * Maps a domain {@link VaultProgram} to this full detail response DTO.
     */
    public static VaultProgramDetailResponse from(VaultProgram vaultProgram) {
        Program program = vaultProgram.program();
        List<WeekResponse> weekResponses = program.getWeeks().stream()
                .map(WeekResponse::from)
                .toList();

        List<DayAssignmentResponse> dayAssignmentResponses = vaultProgram.dayAssignments().stream()
                .map(DayAssignmentResponse::from)
                .toList();

        return new VaultProgramDetailResponse(
                vaultProgram.id(),
                program.getName(),
                program.getGoal(),
                program.getDurationWeeks(),
                program.getEquipmentProfile(),
                vaultProgram.contentSource(),
                vaultProgram.createdAt(),
                vaultProgram.updatedAt(),
                weekResponses,
                dayAssignmentResponses
        );
    }

    public record DayAssignmentResponse(
            int dayNumber,
            String type,
            String workoutId,
            String activityType,
            Object snapshotData,
            String sourceProgramId,
            Integer sourceWeekNumber,
            Integer sourceDayNumber
    ) {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private static final Logger log = LoggerFactory.getLogger(DayAssignmentResponse.class);

        static DayAssignmentResponse from(DayAssignment da) {
            Object snapshot = null;
            if (da.snapshotData() != null) {
                snapshot = deserializeSnapshot(da.snapshotData());
            }
            return new DayAssignmentResponse(
                    da.dayNumber(),
                    da.type().name().toLowerCase(),
                    da.workoutId() != null ? da.workoutId().toString() : null,
                    da.activityType(),
                    snapshot,
                    da.sourceProgramId() != null ? da.sourceProgramId().toString() : null,
                    da.sourceWeekNumber(),
                    da.sourceDayNumber()
            );
        }

        private static Object deserializeSnapshot(String json) {
            try {
                return OBJECT_MAPPER.readValue(json, Object.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize snapshot data: {}", e.getMessage());
                return null;
            }
        }
    }

    public record WeekResponse(
            int weekNumber,
            List<DayResponse> days
    ) {
        static WeekResponse from(Week week) {
            List<DayResponse> dayResponses = week.getDays().stream()
                    .map(DayResponse::from)
                    .toList();
            return new WeekResponse(week.getWeekNumber(), dayResponses);
        }
    }

    public record DayResponse(
            int dayNumber,
            String label,
            String focusArea,
            Modality modality,
            List<WarmCoolEntryResponse> warmUp,
            List<SectionResponse> sections,
            List<WarmCoolEntryResponse> coolDown,
            String methodologySource
    ) {
        static DayResponse from(Day day) {
            List<WarmCoolEntryResponse> warmUpResponses = day.getWarmUp().stream()
                    .map(WarmCoolEntryResponse::from)
                    .toList();
            List<SectionResponse> sectionResponses = day.getSections().stream()
                    .map(SectionResponse::from)
                    .toList();
            List<WarmCoolEntryResponse> coolDownResponses = day.getCoolDown().stream()
                    .map(WarmCoolEntryResponse::from)
                    .toList();

            return new DayResponse(
                    day.getDayNumber(),
                    day.getLabel(),
                    day.getFocusArea(),
                    day.getModality(),
                    warmUpResponses,
                    sectionResponses,
                    coolDownResponses,
                    day.getMethodologySource()
            );
        }
    }

    public record WarmCoolEntryResponse(
            String movement,
            String instruction
    ) {
        static WarmCoolEntryResponse from(WarmCoolEntry entry) {
            return new WarmCoolEntryResponse(entry.movement(), entry.instruction());
        }
    }

    public record SectionResponse(
            String name,
            SectionType sectionType,
            String format,
            Integer timeCap,
            List<ExerciseResponse> exercises
    ) {
        static SectionResponse from(Section section) {
            List<ExerciseResponse> exerciseResponses = section.getExercises().stream()
                    .map(ExerciseResponse::from)
                    .toList();
            return new SectionResponse(
                    section.getName(),
                    section.getSectionType(),
                    section.getFormat(),
                    section.getTimeCap(),
                    exerciseResponses
            );
        }
    }

    public record ExerciseResponse(
            String name,
            ModalityType modalityType,
            int sets,
            String reps,
            String weight,
            Integer restSeconds,
            String notes
    ) {
        static ExerciseResponse from(Exercise exercise) {
            return new ExerciseResponse(
                    exercise.getName(),
                    exercise.getModalityType(),
                    exercise.getSets(),
                    exercise.getReps(),
                    exercise.getWeight(),
                    exercise.getRestSeconds(),
                    exercise.getNotes()
            );
        }
    }
}
