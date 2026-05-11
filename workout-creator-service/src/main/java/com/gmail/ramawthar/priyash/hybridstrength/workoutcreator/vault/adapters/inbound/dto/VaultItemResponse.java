package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a vault item in listings and search results.
 * Mirrors the {@link VaultItem} domain object fields.
 */
public record VaultItemResponse(
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
     * Maps a domain {@link VaultItem} to this response DTO.
     */
    public static VaultItemResponse from(VaultItem item) {
        return new VaultItemResponse(
                item.id(),
                item.name(),
                item.goal(),
                item.durationWeeks(),
                item.equipmentProfile(),
                item.contentSource(),
                item.createdAt(),
                item.updatedAt()
        );
    }
}
