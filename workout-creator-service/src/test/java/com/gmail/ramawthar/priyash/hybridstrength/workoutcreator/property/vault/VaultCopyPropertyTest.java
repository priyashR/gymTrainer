package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature: workout-creator-service-vault, Property 14: Copy Produces Complete Deep Copy with Correct Metadata
 *
 * Copy has new id, name + " (Copy)", MANUAL source, current timestamps, identical structure.
 *
 * Validates: Requirements 5.1, 5.2, 5.3
 */
class VaultCopyPropertyTest {

    /**
     * Property 14: Copying a program produces a new program where:
     * (a) new ID differs from original
     * (b) name equals original name + " (Copy)"
     * (c) contentSource is MANUAL
     * (d) createdAt and updatedAt are set to current time
     * (e) full program structure is identical to original
     *
     * Validates: Requirements 5.1, 5.2, 5.3
     */
    @Property(tries = 100)
    void copyProgram_producesDeepCopyWithCorrectMetadata(
            @ForAll("program") Program originalProgram,
            @ForAll("contentSource") ContentSource originalSource) {

        VaultProgramRepository repository = Mockito.mock(VaultProgramRepository.class);
        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        UUID originalId = UUID.randomUUID();
        String owner = "copy-owner";
        Instant originalCreatedAt = Instant.parse("2025-01-01T00:00:00Z");
        Instant originalUpdatedAt = Instant.parse("2025-01-02T00:00:00Z");

        VaultProgram original = new VaultProgram(originalId, originalProgram, owner,
                originalSource, originalCreatedAt, originalUpdatedAt);

        when(repository.findByIdAndOwner(eq(originalId), eq(owner)))
                .thenReturn(Optional.of(original));
        when(repository.save(any(VaultProgram.class))).thenAnswer(inv -> {
            VaultProgram saved = inv.getArgument(0);
            return new VaultItem(saved.id(), saved.program().getName(), saved.program().getGoal(),
                    saved.program().getDurationWeeks(), saved.program().getEquipmentProfile(),
                    saved.contentSource(), saved.createdAt(), saved.updatedAt());
        });

        Instant beforeCopy = Instant.now();
        service.copyProgram(originalId, owner);

        ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
        verify(repository).save(captor.capture());
        VaultProgram copy = captor.getValue();

        // (a) New ID differs from original
        assertThat(copy.id()).isNotEqualTo(originalId);

        // (b) Name equals original + " (Copy)"
        assertThat(copy.program().getName()).isEqualTo(originalProgram.getName() + " (Copy)");

        // (c) ContentSource is MANUAL
        assertThat(copy.contentSource()).isEqualTo(ContentSource.MANUAL);

        // (d) Timestamps are current
        assertThat(copy.createdAt()).isAfterOrEqualTo(beforeCopy);
        assertThat(copy.updatedAt()).isAfterOrEqualTo(beforeCopy);
        assertThat(copy.createdAt()).isEqualTo(copy.updatedAt());

        // (e) Program structure is identical (except name)
        assertThat(copy.program().getDurationWeeks()).isEqualTo(originalProgram.getDurationWeeks());
        assertThat(copy.program().getGoal()).isEqualTo(originalProgram.getGoal());
        assertThat(copy.program().getEquipmentProfile()).isEqualTo(originalProgram.getEquipmentProfile());
        assertThat(copy.program().getWeeks()).isEqualTo(originalProgram.getWeeks());

        // Owner is preserved
        assertThat(copy.ownerUserId()).isEqualTo(owner);
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
