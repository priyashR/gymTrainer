package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Summary representation of a program in the Vault listing.
 * Contains only the fields needed for search results and list views.
 * Pure domain object — no framework dependencies.
 */
public record VaultItem(
        UUID id,
        String name,
        String goal,
        int durationWeeks,
        List<String> equipmentProfile,
        ContentSource contentSource,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Defensive copy of equipmentProfile to ensure immutability.
     */
    public VaultItem {
        equipmentProfile = equipmentProfile != null ? List.copyOf(equipmentProfile) : List.of();
    }
}
