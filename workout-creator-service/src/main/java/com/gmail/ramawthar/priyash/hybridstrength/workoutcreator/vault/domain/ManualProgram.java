package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A manual program domain object representing a user-created training program.
 * Manual programs consist of ordered day assignments, each mapping a day number
 * to either an existing vault workout or an external activity.
 * Pure domain object — no framework dependencies.
 *
 * @param id              the unique identifier of the program
 * @param name            the user-provided program name
 * @param ownerUserId     the ID of the user who owns this program
 * @param contentSource   the content source — always {@link ContentSource#MANUAL} for manual programs
 * @param dayAssignments  the ordered list of day assignments in this program
 * @param createdAt       the timestamp when the program was created
 * @param updatedAt       the timestamp when the program was last updated
 */
public record ManualProgram(
        UUID id,
        String name,
        String ownerUserId,
        ContentSource contentSource,
        List<DayAssignment> dayAssignments,
        Instant createdAt,
        Instant updatedAt
) {}
