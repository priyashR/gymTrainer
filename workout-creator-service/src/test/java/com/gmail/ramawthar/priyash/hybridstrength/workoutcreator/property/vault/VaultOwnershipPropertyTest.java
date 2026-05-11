package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.ProgramAccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application.VaultService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound.VaultProgramRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Feature: workout-creator-service-vault, Property 1: Ownership Enforcement
 *
 * For any program owned by user A, operations by user B (B ≠ A) must throw
 * ProgramAccessDeniedException.
 *
 * Validates: Requirements 1.4, 2.3, 2.4, 3.2, 3.3, 5.4, 5.5
 */
class VaultOwnershipPropertyTest {

    private final VaultProgramRepository repository = Mockito.mock(VaultProgramRepository.class);
    private final UploadParser uploadParser = Mockito.mock(UploadParser.class);
    private final VaultService service = new VaultService(repository, uploadParser);

    /**
     * Property 1: For any program owned by user A, getProgram by user B (B ≠ A)
     * must throw ProgramAccessDeniedException.
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    void getProgram_byNonOwner_throwsProgramAccessDeniedException(
            @ForAll("programId") UUID programId,
            @ForAll("userId") String ownerA,
            @ForAll("userId") String userB) {

        Assume.that(!ownerA.equals(userB));

        when(repository.findByIdAndOwner(eq(programId), eq(userB)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProgram(programId, userB))
                .isInstanceOf(ProgramAccessDeniedException.class);
    }

    /**
     * Property 1: For any program owned by user A, updateProgram by user B (B ≠ A)
     * must throw ProgramAccessDeniedException.
     *
     * Validates: Requirements 2.3, 2.4
     */
    @Property(tries = 100)
    void updateProgram_byNonOwner_throwsProgramAccessDeniedException(
            @ForAll("programId") UUID programId,
            @ForAll("userId") String ownerA,
            @ForAll("userId") String userB) {

        Assume.that(!ownerA.equals(userB));

        Program program = minimalProgram("Test");
        when(uploadParser.parse(any())).thenReturn(new ParseResult.Success(program));
        when(repository.findByIdAndOwner(eq(programId), eq(userB)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProgram(programId, "{}", userB))
                .isInstanceOf(ProgramAccessDeniedException.class);
    }

    /**
     * Property 1: For any program owned by user A, deleteProgram by user B (B ≠ A)
     * must throw ProgramAccessDeniedException.
     *
     * Validates: Requirements 3.2, 3.3
     */
    @Property(tries = 100)
    void deleteProgram_byNonOwner_throwsProgramAccessDeniedException(
            @ForAll("programId") UUID programId,
            @ForAll("userId") String ownerA,
            @ForAll("userId") String userB) {

        Assume.that(!ownerA.equals(userB));

        when(repository.existsByIdAndOwner(eq(programId), eq(userB)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.deleteProgram(programId, userB))
                .isInstanceOf(ProgramAccessDeniedException.class);
    }

    /**
     * Property 1: For any program owned by user A, copyProgram by user B (B ≠ A)
     * must throw ProgramAccessDeniedException.
     *
     * Validates: Requirements 5.4, 5.5
     */
    @Property(tries = 100)
    void copyProgram_byNonOwner_throwsProgramAccessDeniedException(
            @ForAll("programId") UUID programId,
            @ForAll("userId") String ownerA,
            @ForAll("userId") String userB) {

        Assume.that(!ownerA.equals(userB));

        when(repository.findByIdAndOwner(eq(programId), eq(userB)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.copyProgram(programId, userB))
                .isInstanceOf(ProgramAccessDeniedException.class);
    }

    // ── Arbitraries ──────────────────────────────────────────────────────────

    @Provide
    Arbitrary<UUID> programId() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<String> userId() {
        return Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Program minimalProgram(String name) {
        Exercise exercise = new Exercise("Squat", null, 3, "5", null, null, null);
        Section section = new Section("Tier 1", SectionType.STRENGTH, "Sets/Reps", null, java.util.List.of(exercise));
        Day day = new Day(1, "Day 1", "Push", Modality.HYPERTROPHY,
                java.util.List.of(), java.util.List.of(section), java.util.List.of(), null);
        Week week = new Week(1, java.util.List.of(day));
        return new Program(name, 1, "Build strength", java.util.List.of("Barbell"), java.util.List.of(week));
    }
}
