package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.ProgramAccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.UploadValidationException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadValidationError;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application.VaultService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.SearchCriteria;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound.VaultProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
 * Unit tests for {@link VaultService}.
 * Naming convention: MethodName_StateUnderTest_ExpectedBehaviour
 */
@ExtendWith(MockitoExtension.class)
class VaultServiceTest {

    @Mock
    private VaultProgramRepository vaultProgramRepository;

    @Mock
    private UploadParser uploadParser;

    private VaultService service;

    private static final String OWNER_A = "user-owner-aaa";
    private static final String OWNER_B = "user-owner-bbb";
    private static final UUID PROGRAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        service = new VaultService(vaultProgramRepository, uploadParser);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Program minimalProgram(String name) {
        Exercise exercise = new Exercise("Bench Press", null, 4, "6-8", "80kg", 120, null);
        Section section = new Section("Tier 1", SectionType.STRENGTH, "Sets/Reps", null, List.of(exercise));
        Day day = new Day(1, "Push Day", "Push", Modality.HYPERTROPHY,
                List.of(), List.of(section), List.of(), null);
        Week week = new Week(1, List.of(day));
        return new Program(name, 1, "Build strength", List.of("Barbell"), List.of(week));
    }

    private VaultProgram vaultProgram(UUID id, String name, String owner, ContentSource source) {
        return new VaultProgram(id, minimalProgram(name), owner, source, CREATED_AT, UPDATED_AT);
    }

    private VaultItem vaultItem(UUID id, String name, ContentSource source) {
        return new VaultItem(id, name, "Build strength", 1, List.of("Barbell"), source, CREATED_AT, UPDATED_AT);
    }

    // ── listPrograms ──────────────────────────────────────────────────────────

    @Nested
    class ListPrograms {

        @Test
        void listPrograms_ValidOwner_DelegatesToRepository() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<VaultItem> expected = new PageImpl<>(List.of(vaultItem(PROGRAM_ID, "Program A", ContentSource.UPLOADED)));
            when(vaultProgramRepository.findAllByOwner(OWNER_A, pageable)).thenReturn(expected);

            Page<VaultItem> result = service.listPrograms(OWNER_A, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Program A");
            verify(vaultProgramRepository).findAllByOwner(OWNER_A, pageable);
        }

        @Test
        void listPrograms_EmptyVault_ReturnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            when(vaultProgramRepository.findAllByOwner(OWNER_A, pageable))
                    .thenReturn(Page.empty(pageable));

            Page<VaultItem> result = service.listPrograms(OWNER_A, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ── getProgram ────────────────────────────────────────────────────────────

    @Nested
    class GetProgram {

        @Test
        void getProgram_ExistingProgramOwnedByUser_ReturnsVaultProgram() {
            VaultProgram expected = vaultProgram(PROGRAM_ID, "My Program", OWNER_A, ContentSource.UPLOADED);
            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(expected));

            VaultProgram result = service.getProgram(PROGRAM_ID, OWNER_A);

            assertThat(result.id()).isEqualTo(PROGRAM_ID);
            assertThat(result.program().getName()).isEqualTo("My Program");
            assertThat(result.ownerUserId()).isEqualTo(OWNER_A);
        }

        @Test
        void getProgram_ProgramNotFound_ThrowsProgramAccessDeniedException() {
            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProgram(PROGRAM_ID, OWNER_A))
                    .isInstanceOf(ProgramAccessDeniedException.class)
                    .hasMessage("Program not found or access denied");
        }

        @Test
        void getProgram_ProgramOwnedByDifferentUser_ThrowsProgramAccessDeniedException() {
            // Repository returns empty when owner doesn't match
            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_B))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProgram(PROGRAM_ID, OWNER_B))
                    .isInstanceOf(ProgramAccessDeniedException.class);
        }
    }

    // ── updateProgram ─────────────────────────────────────────────────────────

    @Nested
    class UpdateProgram {

        private static final String VALID_JSON = "{\"program_metadata\":{\"program_name\":\"Updated\"}}";

        @Test
        void updateProgram_ValidJson_ReplacesContentAndReturnsUpdatedItem() {
            Program newProgram = minimalProgram("Updated Program");
            VaultProgram existing = vaultProgram(PROGRAM_ID, "Original", OWNER_A, ContentSource.UPLOADED);
            VaultItem savedItem = vaultItem(PROGRAM_ID, "Updated Program", ContentSource.UPLOADED);

            when(uploadParser.parse(VALID_JSON)).thenReturn(new ParseResult.Success(newProgram));
            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(existing));
            when(vaultProgramRepository.save(any(VaultProgram.class))).thenReturn(savedItem);

            VaultItem result = service.updateProgram(PROGRAM_ID, VALID_JSON, OWNER_A);

            assertThat(result.name()).isEqualTo("Updated Program");
        }

        @Test
        void updateProgram_ValidJson_PreservesContentSource() {
            Program newProgram = minimalProgram("Updated");
            VaultProgram existing = vaultProgram(PROGRAM_ID, "Original", OWNER_A, ContentSource.AI_GENERATED);

            when(uploadParser.parse(VALID_JSON)).thenReturn(new ParseResult.Success(newProgram));
            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(existing));
            when(vaultProgramRepository.save(any(VaultProgram.class)))
                    .thenReturn(vaultItem(PROGRAM_ID, "Updated", ContentSource.AI_GENERATED));

            service.updateProgram(PROGRAM_ID, VALID_JSON, OWNER_A);

            ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
            verify(vaultProgramRepository).save(captor.capture());
            assertThat(captor.getValue().contentSource()).isEqualTo(ContentSource.AI_GENERATED);
        }

        @Test
        void updateProgram_ValidJson_PreservesOwnerUserId() {
            Program newProgram = minimalProgram("Updated");
            VaultProgram existing = vaultProgram(PROGRAM_ID, "Original", OWNER_A, ContentSource.UPLOADED);

            when(uploadParser.parse(VALID_JSON)).thenReturn(new ParseResult.Success(newProgram));
            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(existing));
            when(vaultProgramRepository.save(any(VaultProgram.class)))
                    .thenReturn(vaultItem(PROGRAM_ID, "Updated", ContentSource.UPLOADED));

            service.updateProgram(PROGRAM_ID, VALID_JSON, OWNER_A);

            ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
            verify(vaultProgramRepository).save(captor.capture());
            assertThat(captor.getValue().ownerUserId()).isEqualTo(OWNER_A);
        }

        @Test
        void updateProgram_ValidJson_PreservesOriginalId() {
            Program newProgram = minimalProgram("Updated");
            VaultProgram existing = vaultProgram(PROGRAM_ID, "Original", OWNER_A, ContentSource.UPLOADED);

            when(uploadParser.parse(VALID_JSON)).thenReturn(new ParseResult.Success(newProgram));
            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(existing));
            when(vaultProgramRepository.save(any(VaultProgram.class)))
                    .thenReturn(vaultItem(PROGRAM_ID, "Updated", ContentSource.UPLOADED));

            service.updateProgram(PROGRAM_ID, VALID_JSON, OWNER_A);

            ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
            verify(vaultProgramRepository).save(captor.capture());
            assertThat(captor.getValue().id()).isEqualTo(PROGRAM_ID);
        }

        @Test
        void updateProgram_ValidJson_SetsUpdatedAtToCurrentTime() {
            Instant beforeUpdate = Instant.now();
            Program newProgram = minimalProgram("Updated");
            VaultProgram existing = vaultProgram(PROGRAM_ID, "Original", OWNER_A, ContentSource.UPLOADED);

            when(uploadParser.parse(VALID_JSON)).thenReturn(new ParseResult.Success(newProgram));
            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(existing));
            when(vaultProgramRepository.save(any(VaultProgram.class)))
                    .thenReturn(vaultItem(PROGRAM_ID, "Updated", ContentSource.UPLOADED));

            service.updateProgram(PROGRAM_ID, VALID_JSON, OWNER_A);

            ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
            verify(vaultProgramRepository).save(captor.capture());
            assertThat(captor.getValue().updatedAt()).isAfterOrEqualTo(beforeUpdate);
        }

        @Test
        void updateProgram_InvalidJson_ThrowsUploadValidationException() {
            List<UploadValidationError> errors = List.of(
                    new UploadValidationError("program_metadata.duration_weeks", "must be 1 or 4")
            );
            when(uploadParser.parse(VALID_JSON)).thenReturn(new ParseResult.Failure(errors));

            assertThatThrownBy(() -> service.updateProgram(PROGRAM_ID, VALID_JSON, OWNER_A))
                    .isInstanceOf(UploadValidationException.class);
        }

        @Test
        void updateProgram_InvalidJson_RepositoryNeverCalled() {
            List<UploadValidationError> errors = List.of(
                    new UploadValidationError("$", "Not valid JSON")
            );
            when(uploadParser.parse(VALID_JSON)).thenReturn(new ParseResult.Failure(errors));

            assertThatThrownBy(() -> service.updateProgram(PROGRAM_ID, VALID_JSON, OWNER_A))
                    .isInstanceOf(UploadValidationException.class);

            verify(vaultProgramRepository, never()).save(any());
            verify(vaultProgramRepository, never()).findByIdAndOwner(any(), any());
        }

        @Test
        void updateProgram_ProgramNotOwned_ThrowsProgramAccessDeniedException() {
            Program newProgram = minimalProgram("Updated");
            when(uploadParser.parse(VALID_JSON)).thenReturn(new ParseResult.Success(newProgram));
            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_B))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateProgram(PROGRAM_ID, VALID_JSON, OWNER_B))
                    .isInstanceOf(ProgramAccessDeniedException.class);
        }

        @Test
        void updateProgram_ValidJson_PreservesCreatedAt() {
            Program newProgram = minimalProgram("Updated");
            VaultProgram existing = vaultProgram(PROGRAM_ID, "Original", OWNER_A, ContentSource.UPLOADED);

            when(uploadParser.parse(VALID_JSON)).thenReturn(new ParseResult.Success(newProgram));
            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(existing));
            when(vaultProgramRepository.save(any(VaultProgram.class)))
                    .thenReturn(vaultItem(PROGRAM_ID, "Updated", ContentSource.UPLOADED));

            service.updateProgram(PROGRAM_ID, VALID_JSON, OWNER_A);

            ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
            verify(vaultProgramRepository).save(captor.capture());
            assertThat(captor.getValue().createdAt()).isEqualTo(CREATED_AT);
        }
    }

    // ── deleteProgram ─────────────────────────────────────────────────────────

    @Nested
    class DeleteProgram {

        @Test
        void deleteProgram_ExistingProgramOwnedByUser_DeletesSuccessfully() {
            when(vaultProgramRepository.existsByIdAndOwner(PROGRAM_ID, OWNER_A)).thenReturn(true);

            service.deleteProgram(PROGRAM_ID, OWNER_A);

            verify(vaultProgramRepository).deleteByIdAndOwner(PROGRAM_ID, OWNER_A);
        }

        @Test
        void deleteProgram_ProgramNotFound_ThrowsProgramAccessDeniedException() {
            when(vaultProgramRepository.existsByIdAndOwner(PROGRAM_ID, OWNER_A)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteProgram(PROGRAM_ID, OWNER_A))
                    .isInstanceOf(ProgramAccessDeniedException.class);
        }

        @Test
        void deleteProgram_ProgramOwnedByDifferentUser_ThrowsProgramAccessDeniedException() {
            when(vaultProgramRepository.existsByIdAndOwner(PROGRAM_ID, OWNER_B)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteProgram(PROGRAM_ID, OWNER_B))
                    .isInstanceOf(ProgramAccessDeniedException.class);
        }

        @Test
        void deleteProgram_ProgramNotOwned_RepositoryDeleteNeverCalled() {
            when(vaultProgramRepository.existsByIdAndOwner(PROGRAM_ID, OWNER_B)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteProgram(PROGRAM_ID, OWNER_B))
                    .isInstanceOf(ProgramAccessDeniedException.class);

            verify(vaultProgramRepository, never()).deleteByIdAndOwner(any(), any());
        }
    }

    // ── copyProgram ───────────────────────────────────────────────────────────

    @Nested
    class CopyProgram {

        @Test
        void copyProgram_ExistingProgram_ReturnsNewVaultItem() {
            VaultProgram original = vaultProgram(PROGRAM_ID, "Original Program", OWNER_A, ContentSource.UPLOADED);
            VaultItem copiedItem = vaultItem(UUID.randomUUID(), "Original Program (Copy)", ContentSource.MANUAL);

            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(original));
            when(vaultProgramRepository.save(any(VaultProgram.class))).thenReturn(copiedItem);

            VaultItem result = service.copyProgram(PROGRAM_ID, OWNER_A);

            assertThat(result.name()).isEqualTo("Original Program (Copy)");
        }

        @Test
        void copyProgram_ExistingProgram_CopyHasNewId() {
            VaultProgram original = vaultProgram(PROGRAM_ID, "Original", OWNER_A, ContentSource.UPLOADED);

            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(original));
            when(vaultProgramRepository.save(any(VaultProgram.class)))
                    .thenReturn(vaultItem(UUID.randomUUID(), "Original (Copy)", ContentSource.MANUAL));

            service.copyProgram(PROGRAM_ID, OWNER_A);

            ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
            verify(vaultProgramRepository).save(captor.capture());
            assertThat(captor.getValue().id()).isNotEqualTo(PROGRAM_ID);
        }

        @Test
        void copyProgram_ExistingProgram_CopyNameHasCopySuffix() {
            VaultProgram original = vaultProgram(PROGRAM_ID, "Strength Builder", OWNER_A, ContentSource.UPLOADED);

            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(original));
            when(vaultProgramRepository.save(any(VaultProgram.class)))
                    .thenReturn(vaultItem(UUID.randomUUID(), "Strength Builder (Copy)", ContentSource.MANUAL));

            service.copyProgram(PROGRAM_ID, OWNER_A);

            ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
            verify(vaultProgramRepository).save(captor.capture());
            assertThat(captor.getValue().program().getName()).isEqualTo("Strength Builder (Copy)");
        }

        @Test
        void copyProgram_ExistingProgram_CopyContentSourceIsManual() {
            VaultProgram original = vaultProgram(PROGRAM_ID, "Original", OWNER_A, ContentSource.AI_GENERATED);

            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(original));
            when(vaultProgramRepository.save(any(VaultProgram.class)))
                    .thenReturn(vaultItem(UUID.randomUUID(), "Original (Copy)", ContentSource.MANUAL));

            service.copyProgram(PROGRAM_ID, OWNER_A);

            ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
            verify(vaultProgramRepository).save(captor.capture());
            assertThat(captor.getValue().contentSource()).isEqualTo(ContentSource.MANUAL);
        }

        @Test
        void copyProgram_ExistingProgram_CopyTimestampsAreCurrentTime() {
            Instant beforeCopy = Instant.now();
            VaultProgram original = vaultProgram(PROGRAM_ID, "Original", OWNER_A, ContentSource.UPLOADED);

            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(original));
            when(vaultProgramRepository.save(any(VaultProgram.class)))
                    .thenReturn(vaultItem(UUID.randomUUID(), "Original (Copy)", ContentSource.MANUAL));

            service.copyProgram(PROGRAM_ID, OWNER_A);

            ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
            verify(vaultProgramRepository).save(captor.capture());
            VaultProgram saved = captor.getValue();
            assertThat(saved.createdAt()).isAfterOrEqualTo(beforeCopy);
            assertThat(saved.updatedAt()).isAfterOrEqualTo(beforeCopy);
            assertThat(saved.createdAt()).isEqualTo(saved.updatedAt());
        }

        @Test
        void copyProgram_ExistingProgram_CopyPreservesOwner() {
            VaultProgram original = vaultProgram(PROGRAM_ID, "Original", OWNER_A, ContentSource.UPLOADED);

            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(original));
            when(vaultProgramRepository.save(any(VaultProgram.class)))
                    .thenReturn(vaultItem(UUID.randomUUID(), "Original (Copy)", ContentSource.MANUAL));

            service.copyProgram(PROGRAM_ID, OWNER_A);

            ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
            verify(vaultProgramRepository).save(captor.capture());
            assertThat(captor.getValue().ownerUserId()).isEqualTo(OWNER_A);
        }

        @Test
        void copyProgram_ExistingProgram_CopyPreservesProgramStructure() {
            VaultProgram original = vaultProgram(PROGRAM_ID, "Original", OWNER_A, ContentSource.UPLOADED);

            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_A))
                    .thenReturn(Optional.of(original));
            when(vaultProgramRepository.save(any(VaultProgram.class)))
                    .thenReturn(vaultItem(UUID.randomUUID(), "Original (Copy)", ContentSource.MANUAL));

            service.copyProgram(PROGRAM_ID, OWNER_A);

            ArgumentCaptor<VaultProgram> captor = ArgumentCaptor.forClass(VaultProgram.class);
            verify(vaultProgramRepository).save(captor.capture());
            Program copiedProgram = captor.getValue().program();
            Program originalProgram = original.program();

            assertThat(copiedProgram.getDurationWeeks()).isEqualTo(originalProgram.getDurationWeeks());
            assertThat(copiedProgram.getGoal()).isEqualTo(originalProgram.getGoal());
            assertThat(copiedProgram.getEquipmentProfile()).isEqualTo(originalProgram.getEquipmentProfile());
            assertThat(copiedProgram.getWeeks()).isEqualTo(originalProgram.getWeeks());
        }

        @Test
        void copyProgram_ProgramNotOwned_ThrowsProgramAccessDeniedException() {
            when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_B))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.copyProgram(PROGRAM_ID, OWNER_B))
                    .isInstanceOf(ProgramAccessDeniedException.class);
        }
    }

    // ── searchPrograms ────────────────────────────────────────────────────────

    @Nested
    class SearchPrograms {

        @Test
        void searchPrograms_ValidCriteria_DelegatesToRepository() {
            SearchCriteria criteria = new SearchCriteria("strength", null, null);
            Pageable pageable = PageRequest.of(0, 20);
            Page<VaultItem> expected = new PageImpl<>(List.of(
                    vaultItem(PROGRAM_ID, "Strength Builder", ContentSource.UPLOADED)
            ));
            when(vaultProgramRepository.search(criteria, OWNER_A, pageable)).thenReturn(expected);

            Page<VaultItem> result = service.searchPrograms(criteria, OWNER_A, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(vaultProgramRepository).search(criteria, OWNER_A, pageable);
        }

        @Test
        void searchPrograms_EmptyQuery_ThrowsIllegalArgumentException() {
            SearchCriteria criteria = new SearchCriteria("", null, null);
            Pageable pageable = PageRequest.of(0, 20);

            assertThatThrownBy(() -> service.searchPrograms(criteria, OWNER_A, pageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Search query must not be empty");
        }

        @Test
        void searchPrograms_BlankQuery_ThrowsIllegalArgumentException() {
            SearchCriteria criteria = new SearchCriteria("   ", null, null);
            Pageable pageable = PageRequest.of(0, 20);

            assertThatThrownBy(() -> service.searchPrograms(criteria, OWNER_A, pageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Search query must not be empty");
        }

        @Test
        void searchPrograms_NullQuery_DelegatesToRepository() {
            SearchCriteria criteria = new SearchCriteria(null, "Push", null);
            Pageable pageable = PageRequest.of(0, 20);
            when(vaultProgramRepository.search(criteria, OWNER_A, pageable))
                    .thenReturn(Page.empty(pageable));

            service.searchPrograms(criteria, OWNER_A, pageable);

            verify(vaultProgramRepository).search(criteria, OWNER_A, pageable);
        }

        @Test
        void searchPrograms_EmptyQuery_RepositoryNeverCalled() {
            SearchCriteria criteria = new SearchCriteria("", null, null);
            Pageable pageable = PageRequest.of(0, 20);

            assertThatThrownBy(() -> service.searchPrograms(criteria, OWNER_A, pageable))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(vaultProgramRepository);
        }

        @Test
        void searchPrograms_FiltersOnlyNoQuery_DelegatesToRepository() {
            SearchCriteria criteria = new SearchCriteria(null, "Push", "Hypertrophy");
            Pageable pageable = PageRequest.of(0, 20);
            when(vaultProgramRepository.search(criteria, OWNER_A, pageable))
                    .thenReturn(Page.empty(pageable));

            service.searchPrograms(criteria, OWNER_A, pageable);

            verify(vaultProgramRepository).search(criteria, OWNER_A, pageable);
        }
    }
}
