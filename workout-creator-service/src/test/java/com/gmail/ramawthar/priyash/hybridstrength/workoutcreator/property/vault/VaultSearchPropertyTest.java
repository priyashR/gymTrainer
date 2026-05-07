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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Feature: workout-creator-service-vault, Properties 10, 11, 12, 13: Search behavior
 *
 * Property 10: Search Returns Matching Results for Authenticated User Only
 * Property 11: Empty Query Rejected
 * Property 12: Search Relevance Ordering
 * Property 13: Combined Filter Enforcement
 */
class VaultSearchPropertyTest {

    /**
     * Property 10: Search as user A returns only A's programs matching keyword
     * in name or goal (case-insensitive).
     *
     * Validates: Requirements 4.1, 4.2
     */
    @Property(tries = 100)
    void search_returnsOnlyOwnersMatchingPrograms(
            @ForAll("searchKeyword") String keyword) {

        InMemoryVaultProgramRepository repository = new InMemoryVaultProgramRepository();

        // Create programs for userA — some matching, some not
        VaultProgram matchingByName = createProgram("userA", keyword + " Builder", "General fitness", "Push", Modality.HYPERTROPHY, 0);
        VaultProgram matchingByGoal = createProgram("userA", "Some Program", "Achieve " + keyword + " goals", "Pull", Modality.CROSSFIT, 1);
        VaultProgram nonMatching = createProgram("userA", "000111", "No match here", "Legs", Modality.HYPERTROPHY, 2);

        // Create programs for userB — matching keyword but different owner
        VaultProgram otherUserMatching = createProgram("userB", keyword + " Plan", "Build " + keyword, "Push", Modality.CROSSFIT, 3);

        repository.store(matchingByName);
        repository.store(matchingByGoal);
        repository.store(nonMatching);
        repository.store(otherUserMatching);

        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        SearchCriteria criteria = new SearchCriteria(keyword, null, null);
        Pageable pageable = PageRequest.of(0, 100);
        Page<VaultItem> result = service.searchPrograms(criteria, "userA", pageable);

        // All results belong to userA
        result.getContent().forEach(item ->
                assertThat(repository.getOwner(item.id())).isEqualTo("userA"));

        // All results match keyword in name or goal (case-insensitive)
        result.getContent().forEach(item ->
                assertThat(
                        item.name().toLowerCase().contains(keyword.toLowerCase()) ||
                        item.goal().toLowerCase().contains(keyword.toLowerCase())
                ).isTrue());

        // Should find the two matching programs for userA
        assertThat(result.getContent()).hasSize(2);
    }

    /**
     * Property 11: Any whitespace-only string as q parameter results in IllegalArgumentException.
     *
     * Validates: Requirements 4.3
     */
    @Property(tries = 100)
    void search_emptyOrBlankQuery_throwsIllegalArgumentException(
            @ForAll("whitespaceString") String blankQuery) {

        VaultProgramRepository repository = Mockito.mock(VaultProgramRepository.class);
        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        SearchCriteria criteria = new SearchCriteria(blankQuery, null, null);
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.searchPrograms(criteria, "anyUser", pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Search query must not be empty");
    }

    /**
     * Property 12: Name-matching programs appear before goal-only-matching programs,
     * ties broken by createdAt desc.
     *
     * Validates: Requirements 4.7
     */
    @Property(tries = 100)
    void search_nameMatchesBeforeGoalMatches_orderedByCreatedAtDesc(
            @ForAll("searchKeyword") String keyword) {

        InMemoryVaultProgramRepository repository = new InMemoryVaultProgramRepository();

        // Use names guaranteed not to contain the keyword by using a fixed numeric prefix
        // The keyword is alpha-only (3-8 chars), so purely numeric names won't match
        String safeName1 = "000111";
        String safeName2 = "222333";

        // Goal-only match (created earlier)
        VaultProgram goalMatch1 = createProgram("userA", safeName1, "Build " + keyword, "Push", Modality.HYPERTROPHY, 0);
        // Goal-only match (created later)
        VaultProgram goalMatch2 = createProgram("userA", safeName2, keyword + " focused", "Pull", Modality.CROSSFIT, 2);
        // Name match (created earliest)
        VaultProgram nameMatch1 = createProgram("userA", keyword + " Program", "General", "Push", Modality.HYPERTROPHY, 1);
        // Name match (created latest)
        VaultProgram nameMatch2 = createProgram("userA", "My " + keyword, "Fitness", "Legs", Modality.CROSSFIT, 3);

        repository.store(goalMatch1);
        repository.store(goalMatch2);
        repository.store(nameMatch1);
        repository.store(nameMatch2);

        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        SearchCriteria criteria = new SearchCriteria(keyword, null, null);
        Pageable pageable = PageRequest.of(0, 100);
        Page<VaultItem> result = service.searchPrograms(criteria, "userA", pageable);

        List<VaultItem> items = result.getContent();
        assertThat(items).hasSize(4);

        // Name matches come first
        assertThat(items.get(0).name().toLowerCase()).contains(keyword.toLowerCase());
        assertThat(items.get(1).name().toLowerCase()).contains(keyword.toLowerCase());

        // Within name matches, ordered by createdAt desc
        assertThat(items.get(0).createdAt()).isAfterOrEqualTo(items.get(1).createdAt());

        // Goal-only matches come after
        assertThat(items.get(2).name().toLowerCase()).doesNotContain(keyword.toLowerCase());
        assertThat(items.get(3).name().toLowerCase()).doesNotContain(keyword.toLowerCase());

        // Within goal matches, ordered by createdAt desc
        assertThat(items.get(2).createdAt()).isAfterOrEqualTo(items.get(3).createdAt());
    }

    /**
     * Property 13: Every result matches all specified filter criteria (focusArea, modality).
     *
     * Validates: Requirements 4.8, 4.9, 4.10, 4.11
     */
    @Property(tries = 100)
    void search_withFilters_allResultsMatchFilterCriteria(
            @ForAll("focusArea") String focusArea,
            @ForAll("modality") Modality modality) {

        InMemoryVaultProgramRepository repository = new InMemoryVaultProgramRepository();

        // Program matching both filters
        VaultProgram matching = createProgram("userA", "Matching Program", "Goal", focusArea, modality, 0);
        // Program matching only focusArea
        Modality otherModality = modality == Modality.CROSSFIT ? Modality.HYPERTROPHY : Modality.CROSSFIT;
        VaultProgram wrongModality = createProgram("userA", "Wrong Modality", "Goal", focusArea, otherModality, 1);
        // Program matching only modality
        VaultProgram wrongFocus = createProgram("userA", "Wrong Focus", "Goal", "OtherArea", modality, 2);
        // Program matching neither
        VaultProgram noMatch = createProgram("userA", "No Match", "Goal", "OtherArea", otherModality, 3);

        repository.store(matching);
        repository.store(wrongModality);
        repository.store(wrongFocus);
        repository.store(noMatch);

        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(repository, uploadParser);

        SearchCriteria criteria = new SearchCriteria(null, focusArea, modality.name());
        Pageable pageable = PageRequest.of(0, 100);
        Page<VaultItem> result = service.searchPrograms(criteria, "userA", pageable);

        // Only the matching program should be returned
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Matching Program");
    }

    // ── Arbitraries ──────────────────────────────────────────────────────────

    @Provide
    Arbitrary<String> searchKeyword() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(8)
                .filter(s -> !s.isBlank());
    }

    @Provide
    Arbitrary<String> whitespaceString() {
        return Arbitraries.of("", " ", "  ", "\t", "\n", "   \t  ");
    }

    @Provide
    Arbitrary<String> focusArea() {
        return Arbitraries.of("Push", "Pull", "Legs", "FullBody");
    }

    @Provide
    Arbitrary<Modality> modality() {
        return Arbitraries.of(Modality.values());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private VaultProgram createProgram(String owner, String name, String goal,
                                       String focusArea, Modality modality, int timeOffset) {
        Exercise exercise = new Exercise("Squat", null, 3, "5", null, null, null);
        Section section = new Section("Tier 1", SectionType.STRENGTH, "Sets/Reps", null, List.of(exercise));
        Day day = new Day(1, "Day 1", focusArea, modality,
                List.of(), List.of(section), List.of(), null);
        Week week = new Week(1, List.of(day));
        Program program = new Program(name, 1, goal, List.of("Barbell"), List.of(week));

        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z").plusSeconds(timeOffset * 60L);
        return new VaultProgram(UUID.randomUUID(), program, owner, ContentSource.UPLOADED, createdAt, createdAt);
    }

    // ── In-Memory Repository ─────────────────────────────────────────────────

    private static class InMemoryVaultProgramRepository implements VaultProgramRepository {

        private final Map<UUID, VaultProgram> store = new LinkedHashMap<>();

        void store(VaultProgram program) {
            store.put(program.id(), program);
        }

        String getOwner(UUID id) {
            VaultProgram p = store.get(id);
            return p != null ? p.ownerUserId() : null;
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
            List<VaultProgram> filtered = store.values().stream()
                    .filter(p -> p.ownerUserId().equals(ownerUserId))
                    .filter(p -> matchesKeyword(p, criteria))
                    .filter(p -> matchesFocusArea(p, criteria))
                    .filter(p -> matchesModality(p, criteria))
                    .collect(Collectors.toList());

            // Sort by relevance: name match first, then createdAt desc
            filtered.sort((a, b) -> {
                boolean aNameMatch = criteria.hasKeyword() &&
                        a.program().getName().toLowerCase().contains(criteria.query().toLowerCase());
                boolean bNameMatch = criteria.hasKeyword() &&
                        b.program().getName().toLowerCase().contains(criteria.query().toLowerCase());

                if (aNameMatch && !bNameMatch) return -1;
                if (!aNameMatch && bNameMatch) return 1;
                return b.createdAt().compareTo(a.createdAt());
            });

            List<VaultItem> items = filtered.stream()
                    .map(this::toVaultItem)
                    .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), items.size());
            List<VaultItem> pageContent = start < items.size() ? items.subList(start, end) : List.of();
            return new PageImpl<>(pageContent, pageable, items.size());
        }

        private boolean matchesKeyword(VaultProgram p, SearchCriteria criteria) {
            if (!criteria.hasKeyword()) return true;
            String query = criteria.query().toLowerCase();
            return p.program().getName().toLowerCase().contains(query) ||
                   p.program().getGoal().toLowerCase().contains(query);
        }

        private boolean matchesFocusArea(VaultProgram p, SearchCriteria criteria) {
            if (!criteria.hasFocusArea()) return true;
            String filter = criteria.focusArea().toLowerCase();
            return p.program().getWeeks().stream()
                    .flatMap(w -> w.getDays().stream())
                    .anyMatch(d -> d.getFocusArea().toLowerCase().equals(filter));
        }

        private boolean matchesModality(VaultProgram p, SearchCriteria criteria) {
            if (!criteria.hasModality()) return true;
            String filter = criteria.modality().toLowerCase();
            return p.program().getWeeks().stream()
                    .flatMap(w -> w.getDays().stream())
                    .anyMatch(d -> d.getModality().name().toLowerCase().equals(filter));
        }

        private VaultItem toVaultItem(VaultProgram p) {
            return new VaultItem(p.id(), p.program().getName(), p.program().getGoal(),
                    p.program().getDurationWeeks(), p.program().getEquipmentProfile(),
                    p.contentSource(), p.createdAt(), p.updatedAt());
        }
    }
}
