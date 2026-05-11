package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.UploadValidationException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadValidationError;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application.VaultService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound.VaultProgramRepository;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Feature: workout-creator-service-vault, Properties 6, 7, 8: Update behavior
 *
 * Property 6: Update Replaces Content
 * Property 7: Update Preserves Immutable Fields and Sets Timestamp
 * Property 8: Invalid JSON Rejected on Update
 */
class VaultUpdatePropertyTest {

    /**
     * Property 6: For any existing program and valid new program, update results in
     * stored content matching the new program while preserving the original id.
     *
     * Validates: Requirements 2.1
     */
    @Property(tries = 100)
    void updateProgram_validJson_replacesContentPreservesId(
            @ForAll("program") Program originalProgram,
            @ForAll("program") Program newProgram,
            @ForAll("contentSource") ContentSource contentSource) {

        VaultProgramRepository repository = Mockito.mock(VaultProgramRepository.class);
        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        UUID programId = UUID.randomUUID();
        String owner = "owner-user";
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2025-01-02T00:00:00Z");

        VaultProgram existing = new VaultProgram(programId, originalProgram, owner, contentSource, createdAt, updatedAt);

        when(uploadParser.parse(any())).thenReturn(new ParseResult.Success(newProgram));
        when(repository.findByIdAndOwner(eq(programId), eq(owner))).thenReturn(Optional.of(existing));
        when(repository.save(any(VaultProgram.class))).thenAnswer(inv -> {
            VaultProgram saved = inv.getArgument(0);
            return new VaultItem(saved.id(), saved.program().getName(), saved.program().getGoal(),
                    saved.program().getDurationWeeks(), saved.program().getEquipmentProfile(),
                    saved.contentSource(), saved.createdAt(), saved.updatedAt());
        });

        service.updateProgram(programId, "{}", owner);

        ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
        verify(repository).save(captor.capture());
        VaultProgram saved = captor.getValue();

        // Content is replaced
        assertThat(saved.program()).isEqualTo(newProgram);
        // ID is preserved
        assertThat(saved.id()).isEqualTo(programId);
    }

    /**
     * Property 7: For any update, contentSource and ownerUserId remain unchanged,
     * and updatedAt >= pre-update time.
     *
     * Validates: Requirements 2.2, 2.7
     */
    @Property(tries = 100)
    void updateProgram_preservesImmutableFields_setsTimestamp(
            @ForAll("program") Program originalProgram,
            @ForAll("program") Program newProgram,
            @ForAll("contentSource") ContentSource contentSource) {

        VaultProgramRepository repository = Mockito.mock(VaultProgramRepository.class);
        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        UUID programId = UUID.randomUUID();
        String owner = "immutable-owner";
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
        Instant oldUpdatedAt = Instant.parse("2025-01-02T00:00:00Z");

        VaultProgram existing = new VaultProgram(programId, originalProgram, owner, contentSource, createdAt, oldUpdatedAt);

        when(uploadParser.parse(any())).thenReturn(new ParseResult.Success(newProgram));
        when(repository.findByIdAndOwner(eq(programId), eq(owner))).thenReturn(Optional.of(existing));
        when(repository.save(any(VaultProgram.class))).thenAnswer(inv -> {
            VaultProgram saved = inv.getArgument(0);
            return new VaultItem(saved.id(), saved.program().getName(), saved.program().getGoal(),
                    saved.program().getDurationWeeks(), saved.program().getEquipmentProfile(),
                    saved.contentSource(), saved.createdAt(), saved.updatedAt());
        });

        Instant beforeUpdate = Instant.now();
        service.updateProgram(programId, "{}", owner);

        ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
        verify(repository).save(captor.capture());
        VaultProgram saved = captor.getValue();

        // Immutable fields preserved
        assertThat(saved.contentSource()).isEqualTo(contentSource);
        assertThat(saved.ownerUserId()).isEqualTo(owner);
        assertThat(saved.createdAt()).isEqualTo(createdAt);

        // updatedAt is set to current time
        assertThat(saved.updatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    /**
     * Property 8: For any JSON failing validation, update throws UploadValidationException
     * with at least one error, and the existing program remains unchanged.
     *
     * Validates: Requirements 2.5
     */
    @Property(tries = 100)
    void updateProgram_invalidJson_throwsValidationException_programUnchanged(
            @ForAll("errorField") String errorField,
            @ForAll("errorMessage") String errorMessage) {

        VaultProgramRepository repository = Mockito.mock(VaultProgramRepository.class);
        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        UUID programId = UUID.randomUUID();
        String owner = "owner-user";

        List<UploadValidationError> errors = List.of(new UploadValidationError(errorField, errorMessage));
        when(uploadParser.parse(any())).thenReturn(new ParseResult.Failure(errors));

        assertThatThrownBy(() -> service.updateProgram(programId, "{invalid}", owner))
                .isInstanceOf(UploadValidationException.class)
                .satisfies(ex -> {
                    UploadValidationException uve = (UploadValidationException) ex;
                    assertThat(uve.getErrors()).isNotEmpty();
                    assertThat(uve.getErrors()).hasSize(1);
                });

        // Repository never called — program unchanged
        verify(repository, never()).save(any());
        verify(repository, never()).findByIdAndOwner(any(), any());
    }

    // ── Arbitraries ──────────────────────────────────────────────────────────

    @Provide
    Arbitrary<Program> program() {
        return Combinators.combine(
                nonBlankAlpha(),
                Arbitraries.of(1, 4),
                nonBlankAlpha(),
                equipmentProfile(),
                weeks()
        ).as(Program::new);
    }

    @Provide
    Arbitrary<ContentSource> contentSource() {
        return Arbitraries.of(ContentSource.values());
    }

    @Provide
    Arbitrary<String> errorField() {
        return Arbitraries.of("program_metadata.name", "program_structure[0].days", "$.root", "duration_weeks");
    }

    @Provide
    Arbitrary<String> errorMessage() {
        return Arbitraries.of("is required", "must be an integer", "must not be blank", "invalid format");
    }

    private Arbitrary<List<Week>> weeks() {
        return Arbitraries.of(1, 4).flatMap(numWeeks -> {
            List<Arbitrary<Week>> weekArbs = new java.util.ArrayList<>();
            for (int i = 1; i <= numWeeks; i++) {
                int weekNum = i;
                weekArbs.add(days().map(days -> new Week(weekNum, days)));
            }
            return Combinators.combine(weekArbs).as(list -> list);
        });
    }

    private Arbitrary<List<Day>> days() {
        return Arbitraries.integers().between(1, 2).flatMap(numDays -> {
            List<Arbitrary<Day>> dayArbs = new java.util.ArrayList<>();
            for (int i = 1; i <= numDays; i++) {
                int dayNum = i;
                dayArbs.add(day(dayNum));
            }
            return Combinators.combine(dayArbs).as(list -> list);
        });
    }

    private Arbitrary<Day> day(int dayNumber) {
        return Combinators.combine(
                nonBlankAlpha(),
                nonBlankAlpha(),
                Arbitraries.of(Modality.values()),
                sections()
        ).as((label, focusArea, modality, sections) ->
                new Day(dayNumber, label, focusArea, modality, List.of(), sections, List.of(), null));
    }

    private Arbitrary<List<Section>> sections() {
        return section().list().ofMinSize(1).ofMaxSize(2);
    }

    private Arbitrary<Section> section() {
        return Combinators.combine(
                nonBlankAlpha(),
                Arbitraries.of(SectionType.values()),
                nonBlankAlpha(),
                exercises()
        ).as((name, sectionType, format, exercises) ->
                new Section(name, sectionType, format, null, exercises));
    }

    private Arbitrary<List<Exercise>> exercises() {
        return exercise().list().ofMinSize(1).ofMaxSize(2);
    }

    private Arbitrary<Exercise> exercise() {
        return Combinators.combine(
                nonBlankAlpha(),
                Arbitraries.integers().between(1, 5),
                nonBlankAlpha()
        ).as((name, sets, reps) ->
                new Exercise(name, null, sets, reps, null, null, null));
    }

    private Arbitrary<List<String>> equipmentProfile() {
        return nonBlankAlpha().list().ofMinSize(1).ofMaxSize(3);
    }

    private Arbitrary<String> nonBlankAlpha() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
    }
}
