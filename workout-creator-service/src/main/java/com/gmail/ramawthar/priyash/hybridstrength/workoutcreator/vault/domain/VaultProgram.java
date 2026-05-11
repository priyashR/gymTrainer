package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Program;

import java.time.Instant;
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
        Instant updatedAt
) {}
