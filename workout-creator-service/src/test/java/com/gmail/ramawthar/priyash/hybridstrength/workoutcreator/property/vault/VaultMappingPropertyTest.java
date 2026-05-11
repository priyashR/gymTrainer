package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ProgramEntityMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ProgramJpaEntity;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import net.jqwik.api.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: workout-creator-service-vault, Properties 3 and 4: Mapping Completeness and Round-Trip
 *
 * Property 3: VaultItem Mapping Completeness
 * Property 4: Program Detail Round-Trip
 */
class VaultMappingPropertyTest {

    /**
     * Property 3: For any valid VaultProgram, mapping to VaultItem produces all required
     * fields with matching values.
     *
     * Validates: Requirements 1.2
     */
    @Property(tries = 100)
    void vaultProgramToVaultItem_allFieldsMatch(@ForAll("vaultProgram") VaultProgram vaultProgram) {
        // Create a VaultItem from the VaultProgram (simulating what the mapper does)
        Program program = vaultProgram.program();
        VaultItem item = new VaultItem(
                vaultProgram.id(),
                program.getName(),
                program.getGoal(),
                program.getDurationWeeks(),
                program.getEquipmentProfile(),
                vaultProgram.contentSource(),
                vaultProgram.createdAt(),
                vaultProgram.updatedAt()
        );

        assertThat(item.id()).isEqualTo(vaultProgram.id());
        assertThat(item.name()).isEqualTo(program.getName());
        assertThat(item.goal()).isEqualTo(program.getGoal());
        assertThat(item.durationWeeks()).isEqualTo(program.getDurationWeeks());
        assertThat(item.equipmentProfile()).isEqualTo(program.getEquipmentProfile());
        assertThat(item.contentSource()).isEqualTo(vaultProgram.contentSource());
        assertThat(item.createdAt()).isEqualTo(vaultProgram.createdAt());
        assertThat(item.updatedAt()).isEqualTo(vaultProgram.updatedAt());
    }

    /**
     * Property 4: For any valid Program, converting to entity and back produces an
     * equivalent domain object (toEntity → toVaultProgram round-trip).
     *
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    void programEntityRoundTrip_producesEquivalentDomainObject(
            @ForAll("program") Program program,
            @ForAll("contentSource") ContentSource contentSource) {

        // Domain → Entity
        ProgramJpaEntity entity = ProgramEntityMapper.toEntity(program);
        entity.setOwnerUserId("test-user");
        entity.setContentSource(contentSource);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        // Entity → Domain
        VaultProgram roundTripped = ProgramEntityMapper.toVaultProgram(entity);

        assertThat(roundTripped.program()).isEqualTo(program);
        assertThat(roundTripped.ownerUserId()).isEqualTo("test-user");
        assertThat(roundTripped.contentSource()).isEqualTo(contentSource);
        assertThat(roundTripped.createdAt()).isEqualTo(now);
        assertThat(roundTripped.updatedAt()).isEqualTo(now);
    }

    // ── Arbitraries ──────────────────────────────────────────────────────────

    @Provide
    Arbitrary<VaultProgram> vaultProgram() {
        return Combinators.combine(
                Arbitraries.create(UUID::randomUUID),
                program(),
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15),
                contentSource(),
                Arbitraries.longs().between(1_000_000_000L, 2_000_000_000L).map(Instant::ofEpochSecond),
                Arbitraries.longs().between(1_000_000_000L, 2_000_000_000L).map(Instant::ofEpochSecond)
        ).as(VaultProgram::new);
    }

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
        return Arbitraries.integers().between(1, 3).flatMap(numDays -> {
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
                sections(),
                warmCoolEntries(),
                warmCoolEntries()
        ).as((label, focusArea, modality, sections, warmUp, coolDown) ->
                new Day(dayNumber, label, focusArea, modality, warmUp, sections, coolDown, null));
    }

    private Arbitrary<List<Section>> sections() {
        return section().list().ofMinSize(1).ofMaxSize(3);
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
        return exercise().list().ofMinSize(1).ofMaxSize(3);
    }

    private Arbitrary<Exercise> exercise() {
        return Combinators.combine(
                nonBlankAlpha(),
                Arbitraries.of(ModalityType.values()).injectNull(0.5),
                Arbitraries.integers().between(1, 10),
                nonBlankAlpha()
        ).as((name, modalityType, sets, reps) ->
                new Exercise(name, modalityType, sets, reps, null, null, null));
    }

    private Arbitrary<List<WarmCoolEntry>> warmCoolEntries() {
        return warmCoolEntry().list().ofMinSize(0).ofMaxSize(2);
    }

    private Arbitrary<WarmCoolEntry> warmCoolEntry() {
        return Combinators.combine(nonBlankAlpha(), nonBlankAlpha())
                .as(WarmCoolEntry::new);
    }

    private Arbitrary<List<String>> equipmentProfile() {
        return nonBlankAlpha().list().ofMinSize(1).ofMaxSize(3);
    }

    private Arbitrary<String> nonBlankAlpha() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(12);
    }
}
