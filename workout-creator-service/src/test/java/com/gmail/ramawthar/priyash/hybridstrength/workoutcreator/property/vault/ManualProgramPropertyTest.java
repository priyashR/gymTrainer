package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.dto.ErrorResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.WorkoutNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.VaultController;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto.CreateManualProgramRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto.DayAssignmentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application.VaultService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DayAssignment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DayAssignmentType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.ManualProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.SearchCriteria;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound.VaultProgramRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for Manual Program creation.
 *
 * Feature: workout-creator-service-manual-program
 * Test class for correctness properties defined in the design document.
 */
class ManualProgramPropertyTest {

    private final ListProgramsUseCase listProgramsUseCase = Mockito.mock(ListProgramsUseCase.class);
    private final GetProgramUseCase getProgramUseCase = Mockito.mock(GetProgramUseCase.class);
    private final UpdateProgramUseCase updateProgramUseCase = Mockito.mock(UpdateProgramUseCase.class);
    private final DeleteProgramUseCase deleteProgramUseCase = Mockito.mock(DeleteProgramUseCase.class);
    private final CopyProgramUseCase copyProgramUseCase = Mockito.mock(CopyProgramUseCase.class);
    private final SearchProgramsUseCase searchProgramsUseCase = Mockito.mock(SearchProgramsUseCase.class);
    private final CreateManualProgramUseCase createManualProgramUseCase = Mockito.mock(CreateManualProgramUseCase.class);
    private final GetProgramDaysUseCase getProgramDaysUseCase = Mockito.mock(GetProgramDaysUseCase.class);

    private final VaultController controller = new VaultController(
            listProgramsUseCase,
            getProgramUseCase,
            updateProgramUseCase,
            deleteProgramUseCase,
            copyProgramUseCase,
            searchProgramsUseCase,
            createManualProgramUseCase,
            getProgramDaysUseCase
    );

    // ── Property 2: Conditional required field validation ─────────────────

    /**
     * Property 2: Conditional required field validation — unrecognised type is rejected.
     *
     * For any day assignment where type is not "activity" or "copied_day",
     * the creation request SHALL be rejected with 400 Bad Request.
     *
     * **Validates: Requirements 8.3**
     */
    @Property(tries = 100)
    void conditionalRequiredFieldsEnforced_unrecognisedTypeIsRejected(
            @ForAll("validProgramName") String programName,
            @ForAll("positiveDayNumber") int dayNumber) {

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("test-user", null, "ROLE_USER"));

        try {
            DayAssignmentRequest dayAssignment = new DayAssignmentRequest(
                    dayNumber, "workout", UUID.randomUUID().toString(), null, null, null, null
            );

            CreateManualProgramRequest request = new CreateManualProgramRequest(
                    programName, List.of(dayAssignment)
            );

            MockHttpServletRequest httpRequest = new MockHttpServletRequest("POST", "/api/v1/vault/programs");

            ResponseEntity<?> response = controller.createManualProgram(request, httpRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
            ErrorResponse errorBody = (ErrorResponse) response.getBody();
            assertThat(errorBody.message()).contains("Valid types are: activity, copied_day");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Property 2: Conditional required field validation — activity type with null/blank activityType.
     *
     * For any day assignment where type is "activity" and activityType is null or blank,
     * the creation request SHALL be rejected with 400 Bad Request.
     *
     * **Validates: Requirements 3.2, 3.3**
     */
    @Property(tries = 100)
    void conditionalRequiredFieldsEnforced_activityTypeWithNullOrBlankActivityType(
            @ForAll("nullOrBlankString") String activityType,
            @ForAll("validProgramName") String programName,
            @ForAll("positiveDayNumber") int dayNumber) {

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("test-user", null, "ROLE_USER"));

        try {
            DayAssignmentRequest dayAssignment = new DayAssignmentRequest(
                    dayNumber, "activity", null, activityType, null, null, null
            );

            CreateManualProgramRequest request = new CreateManualProgramRequest(
                    programName, List.of(dayAssignment)
            );

            MockHttpServletRequest httpRequest = new MockHttpServletRequest("POST", "/api/v1/vault/programs");

            ResponseEntity<?> response = controller.createManualProgram(request, httpRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
            ErrorResponse errorBody = (ErrorResponse) response.getBody();
            assertThat(errorBody.message()).containsIgnoringCase("activity");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ── Property 4: Duplicate day numbers are rejected ──────────────────────

    /**
     * Property 1: Whitespace-only program names are rejected.
     *
     * For any string composed entirely of whitespace characters (including null and empty string),
     * submitting it as the programName in a creation request SHALL result in a validation failure
     * (400 Bad Request) and no program being persisted.
     *
     * Validates: Requirements 2.1
     */
    @Property(tries = 100)
    void whitespaceNameIsRejected(@ForAll("whitespaceOnlyNames") String programName) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // Build a valid day assignment so only programName triggers validation failure
        DayAssignmentRequest validDay = new DayAssignmentRequest(
                1, "activity", null, "Running", null, null, null);

        CreateManualProgramRequest request = new CreateManualProgramRequest(
                programName, List.of(validDay));

        Set<ConstraintViolation<CreateManualProgramRequest>> violations = validator.validate(request);

        assertThat(violations)
                .as("programName='%s' (length=%d) must trigger validation failure",
                        programName == null ? "null" : programName.replace("\t", "\\t").replace("\n", "\\n"),
                        programName == null ? 0 : programName.length())
                .isNotEmpty();

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("programName");
    }

    /**
     * Property 4: Duplicate day numbers are rejected.
     *
     * For any list of day assignments containing at least one duplicated dayNumber,
     * the controller must return 400 Bad Request.
     *
     * Validates: Requirements 3.5
     */
    @Property(tries = 100)
    void duplicateDayNumbersRejected(
            @ForAll("dayAssignmentsWithDuplicates") List<DayAssignmentRequest> days) {

        // Set up security context so resolveOwnerUserId() works
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("test-user", null, "ROLE_USER"));

        try {
            CreateManualProgramRequest request = new CreateManualProgramRequest("Valid Program Name", days);
            MockHttpServletRequest httpRequest = new MockHttpServletRequest("POST", "/api/v1/vault/programs");

            ResponseEntity<?> response = controller.createManualProgram(request, httpRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ── Property 3: Non-positive day numbers are rejected ──────────────────

    /**
     * Property 3: Non-positive day numbers are rejected.
     *
     * For any day assignment with a dayNumber less than 1 (zero or negative),
     * the creation request SHALL be rejected with a validation error (400 Bad Request).
     *
     * Validates: Requirements 3.4
     */
    @Property(tries = 100)
    void nonPositiveDayNumbersRejected(
            @ForAll("nonPositiveDayNumber") int dayNumber) {

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        DayAssignmentRequest dayAssignment = new DayAssignmentRequest(
                dayNumber,
                "activity",
                null,
                "Running",
                null,
                null,
                null
        );

        CreateManualProgramRequest request = new CreateManualProgramRequest(
                "Valid Program Name",
                List.of(dayAssignment)
        );

        Set<ConstraintViolation<CreateManualProgramRequest>> violations = validator.validate(request);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().equals("Day number must be positive"));
    }

    // ── Property 5: Non-existent workout references are rejected ────────────

    /**
     * Property 5: Non-existent workout references are rejected.
     *
     * For any day assignment of type "workout" referencing a workoutId that does not exist
     * in the authenticated user's vault, the service SHALL reject the request by throwing
     * WorkoutNotFoundException and the error message SHALL identify the specific workout ID
     * that was not found.
     *
     * Validates: Requirements 4.1, 4.2
     */
    @Property(tries = 100)
    void nonExistentWorkoutReferencesRejected(
            @ForAll("validProgramName") String programName,
            @ForAll("ownerUserId") String ownerUserId,
            @ForAll("workoutDayAssignments") List<DayAssignment> workoutDays) {

        Assume.that(!workoutDays.isEmpty());

        // Arrange: mock repository to return false for all workout IDs (none exist in user's vault)
        VaultProgramRepository mockRepo = Mockito.mock(VaultProgramRepository.class);
        UploadParser mockParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(mockRepo, mockParser, new com.fasterxml.jackson.databind.ObjectMapper());

        for (DayAssignment day : workoutDays) {
            when(mockRepo.existsByIdAndOwner(eq(day.workoutId()), eq(ownerUserId)))
                    .thenReturn(false);
        }

        CreateManualProgramCommand command = new CreateManualProgramCommand(
                programName, ownerUserId, workoutDays
        );

        // The first non-existent workout ID should trigger the exception
        UUID expectedMissingId = workoutDays.get(0).workoutId();

        // Act & Assert: WorkoutNotFoundException is thrown, identifying the missing ID
        assertThatThrownBy(() -> service.createManualProgram(command))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessageContaining(expectedMissingId.toString());

        // saveManualProgram should never be called when a workout is not found
        verify(mockRepo, never()).saveManualProgram(any());
    }

    // ── Arbitraries ──────────────────────────────────────────────────────────

    @Provide
    Arbitrary<String> whitespaceOnlyNames() {
        // Generate strings that are empty or composed entirely of whitespace characters
        return Arbitraries.oneOf(
                // Empty string
                Arbitraries.just(""),
                // Strings of spaces only
                Arbitraries.integers().between(1, 50)
                        .map(n -> " ".repeat(n)),
                // Strings of tabs only
                Arbitraries.integers().between(1, 20)
                        .map(n -> "\t".repeat(n)),
                // Strings of newlines only
                Arbitraries.integers().between(1, 20)
                        .map(n -> "\n".repeat(n)),
                // Mixed whitespace characters (space, tab, newline, carriage return)
                Arbitraries.strings()
                        .withChars(' ', '\t', '\n', '\r')
                        .ofMinLength(1)
                        .ofMaxLength(30)
        );
    }

    @Provide
    Arbitrary<String> nullOrBlankString() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.just("\t"),
                Arbitraries.just("\n"),
                Arbitraries.just(" \t\n "),
                Arbitraries.strings()
                        .withChars(' ', '\t', '\n', '\r')
                        .ofMinLength(1)
                        .ofMaxLength(10)
        );
    }

    @Provide
    Arbitrary<Integer> positiveDayNumber() {
        return Arbitraries.integers().between(1, 365);
    }

    @Provide
    Arbitrary<Integer> nonPositiveDayNumber() {
        return Arbitraries.integers().lessOrEqual(0);
    }

    @Provide
    Arbitrary<String> validProgramName() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100);
    }

    @Provide
    Arbitrary<String> ownerUserId() {
        return Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20);
    }

    @Provide
    Arbitrary<List<DayAssignment>> workoutDayAssignments() {
        return Arbitraries.integers().between(1, 7).flatMap(size ->
                Arbitraries.integers().between(1, 30).list().ofSize(size).uniqueElements().flatMap(dayNumbers -> {
                    List<Arbitrary<DayAssignment>> dayArbitraries = new ArrayList<>();
                    for (int dayNumber : dayNumbers) {
                        dayArbitraries.add(
                                Arbitraries.create(UUID::randomUUID).map(workoutId ->
                                        new DayAssignment(dayNumber, DayAssignmentType.WORKOUT, workoutId, null)
                                )
                        );
                    }
                    return Combinators.combine(dayArbitraries).as(days -> days);
                })
        );
    }

    /**
     * Generates lists of day assignments where at least two entries share the same dayNumber.
     * Strategy: generate a base list of valid day assignments, then ensure at least one duplicate
     * dayNumber by copying a dayNumber from an existing entry into another entry.
     */
    @Provide
    Arbitrary<List<DayAssignmentRequest>> dayAssignmentsWithDuplicates() {
        return Arbitraries.integers().between(1, 100).flatMap(duplicatedDayNumber ->
            Arbitraries.integers().between(2, 10).flatMap(listSize ->
                Arbitraries.integers().between(0, listSize - 1).flatMap(dupIndex1 ->
                    Arbitraries.integers().between(0, listSize - 1)
                        .filter(dupIndex2 -> !dupIndex2.equals(dupIndex1))
                        .map(dupIndex2 -> {
                            List<DayAssignmentRequest> days = new ArrayList<>();
                            for (int i = 0; i < listSize; i++) {
                                int dayNumber;
                                if (i == dupIndex1 || i == dupIndex2) {
                                    dayNumber = duplicatedDayNumber;
                                } else {
                                    // Use unique day numbers that won't collide with the duplicate
                                    dayNumber = duplicatedDayNumber + 100 + i;
                                }
                                days.add(new DayAssignmentRequest(
                                        dayNumber,
                                        "activity",
                                        null,
                                        "Running",
                                        null,
                                        null,
                                        null
                                ));
                            }
                            return days;
                        })
                )
            )
        );
    }

    // ── Property 6: Manual program persistence round-trip ────────────────────

    /**
     * Property 6: Manual program persistence round-trip.
     *
     * For any valid CreateManualProgramCommand (valid name, valid day assignments with all
     * referenced workouts existing), persisting the manual program and then retrieving it
     * by the returned ID SHALL produce a ManualProgram with:
     * - The same program name
     * - The same owner user ID
     * - contentSource equal to MANUAL
     * - An equivalent set of day assignments (same day numbers, types, workout IDs, and activity types)
     *
     * Validates: Requirements 5.1, 5.2, 5.3
     */
    @Property(tries = 100)
    void persistenceRoundTrip(@ForAll("validCreateManualProgramCommands") CreateManualProgramCommand command) {
        // Arrange: in-memory repository stub that captures persisted programs
        InMemoryVaultProgramRepository inMemoryRepo = new InMemoryVaultProgramRepository();
        UploadParser uploadParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(inMemoryRepo, uploadParser, new com.fasterxml.jackson.databind.ObjectMapper());

        // Act: create the manual program via the service
        UUID programId = service.createManualProgram(command);

        // Assert: retrieve the persisted program and verify round-trip consistency
        assertThat(programId).isNotNull();

        ManualProgram persisted = inMemoryRepo.findManualProgramById(programId);
        assertThat(persisted).isNotNull();

        // Same program name
        assertThat(persisted.name()).isEqualTo(command.programName());

        // Same owner user ID
        assertThat(persisted.ownerUserId()).isEqualTo(command.ownerUserId());

        // contentSource is MANUAL
        assertThat(persisted.contentSource()).isEqualTo(ContentSource.MANUAL);

        // Equivalent set of day assignments
        assertThat(persisted.dayAssignments()).hasSameSizeAs(command.dayAssignments());

        for (int i = 0; i < command.dayAssignments().size(); i++) {
            DayAssignment expected = command.dayAssignments().get(i);
            DayAssignment actual = persisted.dayAssignments().get(i);
            assertThat(actual.dayNumber()).isEqualTo(expected.dayNumber());
            assertThat(actual.type()).isEqualTo(expected.type());
            assertThat(actual.workoutId()).isEqualTo(expected.workoutId());
            assertThat(actual.activityType()).isEqualTo(expected.activityType());
        }
    }

    /**
     * Generates fully valid CreateManualProgramCommand instances:
     * - Non-blank program names of 1-255 characters
     * - 1 to 7 day assignments with unique day numbers >= 1
     * - Each day is either WORKOUT (with a non-null UUID) or ACTIVITY (with a non-blank activityType)
     */
    @Provide
    Arbitrary<CreateManualProgramCommand> validCreateManualProgramCommands() {
        Arbitrary<String> programNames = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50)
                .map(s -> s + " Program"); // Ensure non-blank and reasonable length

        Arbitrary<String> ownerUserIds = Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(20)
                .map(s -> "user-" + s);

        return Combinators.combine(programNames, ownerUserIds, validDayAssignmentLists())
                .as(CreateManualProgramCommand::new);
    }

    private Arbitrary<List<DayAssignment>> validDayAssignmentLists() {
        return Arbitraries.integers().between(1, 7).flatMap(size -> {
            // Generate 'size' unique day numbers and map each to a valid assignment
            return Arbitraries.shuffle(1, 2, 3, 4, 5, 6, 7)
                    .map(shuffled -> shuffled.subList(0, size))
                    .flatMap(dayNumbers -> {
                        List<Arbitrary<DayAssignment>> dayArbitraries = new ArrayList<>();
                        for (int dayNumber : dayNumbers) {
                            dayArbitraries.add(validDayAssignment(dayNumber));
                        }
                        return Combinators.combine(dayArbitraries).as(list -> list);
                    });
        });
    }

    private Arbitrary<DayAssignment> validDayAssignment(int dayNumber) {
        Arbitrary<DayAssignment> workoutDay = Arbitraries.just(dayNumber)
                .map(dn -> new DayAssignment(dn, DayAssignmentType.WORKOUT, UUID.randomUUID(), null));

        Arbitrary<DayAssignment> activityDay = Arbitraries.of("Soccer", "Swimming", "Running", "Cycling", "Yoga", "Basketball")
                .map(activity -> new DayAssignment(dayNumber, DayAssignmentType.ACTIVITY, null, activity));

        return Arbitraries.oneOf(workoutDay, activityDay);
    }

    // ── In-Memory Repository Stub ────────────────────────────────────────────

    /**
     * In-memory stub implementation of VaultProgramRepository for property testing.
     * Stores ManualProgram instances and returns true for all existsByIdAndOwner checks
     * (simulating that all referenced workouts exist in the user's vault).
     */
    private static class InMemoryVaultProgramRepository implements VaultProgramRepository {

        private final Map<UUID, ManualProgram> manualPrograms = new ConcurrentHashMap<>();

        @Override
        public void saveManualProgram(ManualProgram program) {
            manualPrograms.put(program.id(), program);
        }

        ManualProgram findManualProgramById(UUID id) {
            return manualPrograms.get(id);
        }

        @Override
        public boolean existsByIdAndOwner(UUID id, String ownerUserId) {
            // Always return true — simulates all referenced workouts existing in the user's vault
            return true;
        }

        // ── Unused methods (not needed for this property test) ────────────────

        @Override
        public Page<VaultItem> findAllByOwner(String ownerUserId, Pageable pageable) {
            throw new UnsupportedOperationException("Not used in persistence round-trip test");
        }

        @Override
        public Optional<VaultProgram> findByIdAndOwner(UUID id, String ownerUserId) {
            throw new UnsupportedOperationException("Not used in persistence round-trip test");
        }

        @Override
        public VaultItem save(VaultProgram program) {
            throw new UnsupportedOperationException("Not used in persistence round-trip test");
        }

        @Override
        public void deleteByIdAndOwner(UUID id, String ownerUserId) {
            throw new UnsupportedOperationException("Not used in persistence round-trip test");
        }

        @Override
        public Page<VaultItem> search(SearchCriteria criteria, String ownerUserId, Pageable pageable) {
            throw new UnsupportedOperationException("Not used in persistence round-trip test");
        }
    }
}
