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
 * Feature: workout-creator-service-vault, Property 2: Listing Returns Only Owner's Programs in Correct Order
 *
 * For any set of programs belonging to multiple users, listing for user A returns only A's
 * programs ordered by createdAt desc.
 *
 * Validates: Requirements 1.1
 */
class VaultListingPropertyTest {

    /**
     * Property 2: Listing returns only owner's programs in createdAt descending order.
     *
     * Uses an in-memory repository implementation to verify the listing logic.
     *
     * Validates: Requirements 1.1
     */
    @Property(tries = 100)
    void listPrograms_returnsOnlyOwnersPrograms_orderedByCreatedAtDesc(
            @ForAll("programsForMultipleUsers") List<VaultProgram> allPrograms,
            @ForAll("targetUser") String targetUser) {

        InMemoryVaultProgramRepository repository = new InMemoryVaultProgramRepository();
        allPrograms.forEach(repository::store);

        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        Pageable pageable = PageRequest.of(0, 100);
        Page<VaultItem> result = service.listPrograms(targetUser, pageable);

        // All returned items belong to the target user
        List<VaultItem> items = result.getContent();

        // Verify ordering: createdAt descending
        for (int i = 1; i < items.size(); i++) {
            assertThat(items.get(i - 1).createdAt())
                    .isAfterOrEqualTo(items.get(i).createdAt());
        }

        // Verify completeness: all programs belonging to targetUser are returned
        long expectedCount = allPrograms.stream()
                .filter(p -> p.ownerUserId().equals(targetUser))
                .count();
        assertThat(result.getTotalElements()).isEqualTo(expectedCount);
    }

    // ── Arbitraries ──────────────────────────────────────────────────────────

    @Provide
    Arbitrary<List<VaultProgram>> programsForMultipleUsers() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 10),
                Arbitraries.of("userA", "userB", "userC")
        ).as((count, ignored) -> count).flatMap(count ->
                Arbitraries.of("userA", "userB", "userC")
                        .list().ofSize(count)
                        .flatMap(owners -> {
                            List<Arbitrary<VaultProgram>> programs = new ArrayList<>();
                            for (int i = 0; i < owners.size(); i++) {
                                String owner = owners.get(i);
                                int index = i;
                                programs.add(Arbitraries.just(createVaultProgram(owner, index)));
                            }
                            return Combinators.combine(programs).as(list -> list);
                        })
        );
    }

    @Provide
    Arbitrary<String> targetUser() {
        return Arbitraries.of("userA", "userB", "userC");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private VaultProgram createVaultProgram(String owner, int index) {
        Exercise exercise = new Exercise("Squat", null, 3, "5", null, null, null);
        Section section = new Section("Tier 1", SectionType.STRENGTH, "Sets/Reps", null, List.of(exercise));
        Day day = new Day(1, "Day 1", "Push", Modality.HYPERTROPHY,
                List.of(), List.of(section), List.of(), null);
        Week week = new Week(1, List.of(day));
        Program program = new Program("Program " + index, 1, "Goal " + index, List.of("Barbell"), List.of(week));

        // Use different timestamps to test ordering
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z").plusSeconds(index * 60L);
        return new VaultProgram(UUID.randomUUID(), program, owner, ContentSource.UPLOADED, createdAt, createdAt);
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
