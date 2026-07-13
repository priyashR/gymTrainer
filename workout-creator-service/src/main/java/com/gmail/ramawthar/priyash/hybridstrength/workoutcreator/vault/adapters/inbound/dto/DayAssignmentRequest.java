package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO representing a single day assignment within a manual program creation request.
 * Each day maps a day number to either a vault workout (by ID), an external activity (by type name),
 * or a copied day from another program (by source program ID, week number, and day number).
 */
public record DayAssignmentRequest(
        @Min(value = 1, message = "Day number must be positive")
        int dayNumber,

        @NotBlank(message = "Day type is required")
        String type,

        String workoutId,

        String activityType,

        String sourceProgramId,

        Integer sourceWeekNumber,

        Integer sourceDayNumber
) {}
