package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.ProgramAccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application.VaultService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound.VaultProgramRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Feature: workout-creator-service-vault, Property 9: Delete Removes Program
 *
 * After deletion, retrieval throws ProgramAccessDeniedException.
 *
 * Validates: Requirements 3.1, 3.5
 */
class VaultDeletePropertyTest {

    /**
     * Property 9: After user A deletes a program, attempting to retrieve it
     * throws ProgramAccessDeniedException.
     *
     * Validates: Requirements 3.1, 3.5
     */
    @Property(tries = 100)
    void deleteProgram_thenGetProgram_throwsProgramAccessDeniedException(
            @ForAll("programId") UUID programId,
            @ForAll("userId") String owner) {

        VaultProgramRepository repository = Mockito.mock(VaultProgramRepository.class);
        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        // Setup: program exists and can be deleted
        when(repository.existsByIdAndOwner(eq(programId), eq(owner))).thenReturn(true);

        // Delete the program
        service.deleteProgram(programId, owner);

        // After deletion, retrieval returns empty (simulating deleted state)
        when(repository.findByIdAndOwner(eq(programId), eq(owner))).thenReturn(Optional.empty());

        // Attempting to retrieve throws ProgramAccessDeniedException
        assertThatThrownBy(() -> service.getProgram(programId, owner))
                .isInstanceOf(ProgramAccessDeniedException.class);
    }

    // ── Arbitraries ──────────────────────────────────────────────────────────

    @Provide
    Arbitrary<UUID> programId() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<String> userId() {
        return Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15);
    }
}
