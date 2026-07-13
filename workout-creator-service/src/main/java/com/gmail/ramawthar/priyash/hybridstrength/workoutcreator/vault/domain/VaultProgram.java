package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Program;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full program representation with ownership metadata.
 * Used for detail retrieval and copy operations.
 * Pure domain object — no framework dependencies.
 */
public record VaultProgram(
        UUID id,
        Program program,
        String ownerUserId,
        ContentSource contentSource,
        Instant createdAt,
        Instant updatedAt,
        List<DayAssignment> dayAssignments
) {
    /**
     * Backwards-compatible constructor for programs without day assignments (AI/Uploaded).
     */
    public VaultProgram(UUID id, Program program, String ownerUserId,
                        ContentSource contentSource, Instant createdAt, Instant updatedAt) {
        this(id, program, ownerUserId, contentSource, createdAt, updatedAt, List.of());
    }
}
