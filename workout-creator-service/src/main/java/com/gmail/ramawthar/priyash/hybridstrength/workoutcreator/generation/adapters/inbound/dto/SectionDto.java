package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.SectionType;

import java.util.List;

/**
 * API response DTO for a section within a workout.
 */
public record SectionDto(
        String name,
        SectionType type,
        List<ExerciseDto> exercises,
        Integer timeCapMinutes,
        Integer intervalSeconds,
        Integer totalRounds,
        Integer workIntervalSeconds,
        Integer restIntervalSeconds
) {}
