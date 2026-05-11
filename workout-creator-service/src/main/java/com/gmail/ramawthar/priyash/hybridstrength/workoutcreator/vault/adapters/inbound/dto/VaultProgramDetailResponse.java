package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the full program detail endpoint.
 * Includes all nested weeks, days, sections, exercises, and warm-up/cool-down entries.
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
        List<WeekResponse> weeks
) {

    /**
     * Maps a domain {@link VaultProgram} to this full detail response DTO.
     */
    public static VaultProgramDetailResponse from(VaultProgram vaultProgram) {
        Program program = vaultProgram.program();
        List<WeekResponse> weekResponses = program.getWeeks().stream()
                .map(WeekResponse::from)
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
                weekResponses
        );
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
