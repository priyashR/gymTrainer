package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application.VaultService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.SearchCriteria;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound.VaultProgramRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: workout-creator-service-vault, Property 5: Pagination Invariants
 *
 * For any N programs and valid page size S, paginating yields exactly N items
 * with no duplicates/omissions, and each page contains at most S items.
 *
 * Validates: Requirements 1.6, 4.5
 */
class VaultPaginationPropertyTest {

    private static final String OWNER = "pagination-test-user";

    /**
     * Property 5: Paginating through all pages yields exactly N total items with
     * no duplicates and no omissions, and each page has at most S items.
     *
     * Validates: Requirements 1.6, 4.5
     */
    @Property(tries = 100)
    void pagination_yieldsAllItems_noDuplicates_noOmissions(
            @ForAll("programCount") int programCount,
            @ForAll("pageSize") int pageSize) {

        InMemoryVaultProgramRepository repository = new InMemoryVaultProgramRepository();
        List<VaultProgram> programs = createPrograms(programCount);
        programs.forEach(repository::store);

        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        // Collect all items across all pages
        List<VaultItem> allItems = new ArrayList<>();
        int totalPages = (int) Math.ceil((double) programCount / pageSize);
        if (totalPages == 0) totalPages = 1;

        for (int page = 0; page < totalPages; page++) {
            Pageable pageable = PageRequest.of(page, pageSize);
            Page<VaultItem> result = service.listPrograms(OWNER, pageable);

            List<VaultItem> pageContent = result.getContent();

            // Each page has at most pageSize items
            assertThat(pageContent.size()).isLessThanOrEqualTo(pageSize);

            allItems.addAll(pageContent);
        }

        // Total items equals programCount
        assertThat(allItems).hasSize(programCount);

        // No duplicates
        Set<UUID> ids = allItems.stream().map(VaultItem::id).collect(Collectors.toSet());
        assertThat(ids).hasSize(programCount);
    }

    // ── Arbitraries ──────────────────────────────────────────────────────────

    @Provide
    Arbitrary<Integer> programCount() {
        return Arbitraries.integers().between(0, 25);
    }

    @Provide
    Arbitrary<Integer> pageSize() {
        return Arbitraries.integers().between(1, 10);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private List<VaultProgram> createPrograms(int count) {
        List<VaultProgram> programs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Exercise exercise = new Exercise("Squat", null, 3, "5", null, null, null);
            Section section = new Section("Tier 1", SectionType.STRENGTH, "Sets/Reps", null, List.of(exercise));
            Day day = new Day(1, "Day 1", "Push", Modality.HYPERTROPHY,
                    List.of(), List.of(section), List.of(), null);
            Week week = new Week(1, List.of(day));
            Program program = new Program("Program " + i, 1, "Goal", List.of("Barbell"), List.of(week));

            Instant createdAt = Instant.parse("2025-01-01T00:00:00Z").plusSeconds(i * 60L);
            programs.add(new VaultProgram(UUID.randomUUID(), program, OWNER, ContentSource.UPLOADED, createdAt, createdAt));
        }
        return programs;
    }

    // ── In-Memory Repository ─────────────────────────────────────────────────

    private static class InMemoryVaultProgramRepository implements VaultProgramRepository {

        private final Map<UUID, VaultProgram> store = new LinkedHashMap<>();

        void store(VaultProgram program) {
            store.put(program.id(), program);
        }

        @Override
        public Page<VaultItem> findAllByOwner(String ownerUserId, Pageable pageable) {
            List<VaultItem> items = store.values().stream()
                    .filter(p -> p.ownerUserId().equals(ownerUserId))
                    .sorted(Comparator.comparing(VaultProgram::createdAt).reversed())
                    .map(this::toVaultItem)
                    .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), items.size());
            List<VaultItem> pageContent = start < items.size() ? items.subList(start, end) : List.of();
            return new PageImpl<>(pageContent, pageable, items.size());
        }

        @Override
        public Optional<VaultProgram> findByIdAndOwner(UUID id, String ownerUserId) {
            return Optional.ofNullable(store.get(id))
                    .filter(p -> p.ownerUserId().equals(ownerUserId));
        }

        @Override
        public VaultItem save(VaultProgram program) {
            store.put(program.id(), program);
            return toVaultItem(program);
        }

        @Override
        public void deleteByIdAndOwner(UUID id, String ownerUserId) {
            store.entrySet().removeIf(e ->
                    e.getKey().equals(id) && e.getValue().ownerUserId().equals(ownerUserId));
        }

        @Override
        public boolean existsByIdAndOwner(UUID id, String ownerUserId) {
            return store.containsKey(id) && store.get(id).ownerUserId().equals(ownerUserId);
        }

        @Override
        public Page<VaultItem> search(SearchCriteria criteria, String ownerUserId, Pageable pageable) {
            return Page.empty(pageable);
        }

        private VaultItem toVaultItem(VaultProgram p) {
            return new VaultItem(p.id(), p.program().getName(), p.program().getGoal(),
                    p.program().getDurationWeeks(), p.program().getEquipmentProfile(),
                    p.contentSource(), p.createdAt(), p.updatedAt());
        }
    }
}
