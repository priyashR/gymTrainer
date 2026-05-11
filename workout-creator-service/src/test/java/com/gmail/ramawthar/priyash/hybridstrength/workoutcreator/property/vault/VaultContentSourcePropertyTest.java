package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
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
 * Feature: workout-creator-service-vault, Property 15: Content Source Does Not Affect Behavior
 *
 * Two programs with identical structure but different contentSource produce equivalent
 * results for all operations (differing only in the contentSource field itself).
 *
 * Validates: Requirements 6.4
 */
class VaultContentSourcePropertyTest {

    /**
     * Property 15: For two programs with identical structure but different contentSource,
     * getProgram produces equivalent results (differing only in contentSource).
     *
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    void getProgram_differentContentSources_producesEquivalentResults(
            @ForAll("program") Program program,
            @ForAll("twoContentSources") List<ContentSource> sources) {

        ContentSource sourceA = sources.get(0);
        ContentSource sourceB = sources.get(1);
        Assume.that(sourceA != sourceB);

        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        String owner = "content-source-user";
        Instant now = Instant.now();

        VaultProgram programA = new VaultProgram(idA, program, owner, sourceA, now, now);
        VaultProgram programB = new VaultProgram(idB, program, owner, sourceB, now, now);

        VaultProgramRepository repository = Mockito.mock(VaultProgramRepository.class);
        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        when(repository.findByIdAndOwner(eq(idA), eq(owner))).thenReturn(Optional.of(programA));
        when(repository.findByIdAndOwner(eq(idB), eq(owner))).thenReturn(Optional.of(programB));

        VaultProgram resultA = service.getProgram(idA, owner);
        VaultProgram resultB = service.getProgram(idB, owner);

        // Programs are equivalent except for id and contentSource
        assertThat(resultA.program()).isEqualTo(resultB.program());
        assertThat(resultA.ownerUserId()).isEqualTo(resultB.ownerUserId());
        assertThat(resultA.createdAt()).isEqualTo(resultB.createdAt());
        assertThat(resultA.updatedAt()).isEqualTo(resultB.updatedAt());
        // ContentSource differs as expected
        assertThat(resultA.contentSource()).isNotEqualTo(resultB.contentSource());
    }

    /**
     * Property 15: For two programs with identical structure but different contentSource,
     * copyProgram produces equivalent copy structures (both become MANUAL).
     *
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    void copyProgram_differentContentSources_producesEquivalentCopies(
            @ForAll("program") Program program,
            @ForAll("twoContentSources") List<ContentSource> sources) {

        ContentSource sourceA = sources.get(0);
        ContentSource sourceB = sources.get(1);
        Assume.that(sourceA != sourceB);

        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        String owner = "content-source-user";
        Instant now = Instant.now();

        VaultProgram programA = new VaultProgram(idA, program, owner, sourceA, now, now);
        VaultProgram programB = new VaultProgram(idB, program, owner, sourceB, now, now);

        VaultProgramRepository repositoryA = Mockito.mock(VaultProgramRepository.class);
        VaultProgramRepository repositoryB = Mockito.mock(VaultProgramRepository.class);
        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService serviceA = new VaultService(repositoryA, uploadParser);
        VaultService serviceB = new VaultService(repositoryB, uploadParser);

        when(repositoryA.findByIdAndOwner(eq(idA), eq(owner))).thenReturn(Optional.of(programA));
        when(repositoryA.save(any(VaultProgram.class))).thenAnswer(inv -> {
            VaultProgram saved = inv.getArgument(0);
            return new VaultItem(saved.id(), saved.program().getName(), saved.program().getGoal(),
                    saved.program().getDurationWeeks(), saved.program().getEquipmentProfile(),
                    saved.contentSource(), saved.createdAt(), saved.updatedAt());
        });

        when(repositoryB.findByIdAndOwner(eq(idB), eq(owner))).thenReturn(Optional.of(programB));
        when(repositoryB.save(any(VaultProgram.class))).thenAnswer(inv -> {
            VaultProgram saved = inv.getArgument(0);
            return new VaultItem(saved.id(), saved.program().getName(), saved.program().getGoal(),
                    saved.program().getDurationWeeks(), saved.program().getEquipmentProfile(),
                    saved.contentSource(), saved.createdAt(), saved.updatedAt());
        });

        serviceA.copyProgram(idA, owner);
        serviceB.copyProgram(idB, owner);

        ArgumentCaptor<VaultProgram> captorA = ArgumentCaptor.forClass(VaultProgram.class);
        ArgumentCaptor<VaultProgram> captorB = ArgumentCaptor.forClass(VaultProgram.class);
        verify(repositoryA).save(captorA.capture());
        verify(repositoryB).save(captorB.capture());

        VaultProgram copyA = captorA.getValue();
        VaultProgram copyB = captorB.getValue();

        // Both copies have MANUAL source regardless of original
        assertThat(copyA.contentSource()).isEqualTo(ContentSource.MANUAL);
        assertThat(copyB.contentSource()).isEqualTo(ContentSource.MANUAL);

        // Both copies have equivalent program structure
        assertThat(copyA.program()).isEqualTo(copyB.program());
        assertThat(copyA.ownerUserId()).isEqualTo(copyB.ownerUserId());
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
    Arbitrary<List<ContentSource>> twoContentSources() {
        return Arbitraries.of(ContentSource.values())
                .list().ofSize(2)
                .filter(list -> list.get(0) != list.get(1));
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
