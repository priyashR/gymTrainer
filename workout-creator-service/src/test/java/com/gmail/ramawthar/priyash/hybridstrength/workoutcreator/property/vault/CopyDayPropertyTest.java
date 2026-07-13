package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.dto.ErrorResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.SourceDayNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.SourceProgramNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.VaultController;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto.CreateManualProgramRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto.DayAssignmentRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application.VaultService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.*;
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

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for Copy Day to Manual Program feature.
 *
 * Feature: copy-day-to-manual-program
 * Test class for correctness properties defined in the design document.
 */
class CopyDayPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule());

    // ── Property 1: Snapshot serialization round-trip ────────────────────────

    /**
     * Property 1: Snapshot serialization round-trip.
     *
     * For any valid Day domain object (with arbitrary label, focus area, modality,
     * warm-up entries, sections with exercises including nullable fields, and cool-down entries),
     * serializing it to JSON and then deserializing it back SHALL produce a Day object with
     * identical field values for all fields defined in the snapshot structure.
     *
     * **Validates: Requirements 4.2, 7.2**
     */
    @Property(tries = 100)
    void snapshotSerializationRoundTrip(@ForAll("arbitraryDay") Day day) throws Exception {
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(day);

        // Deserialize back
        Day deserialized = objectMapper.readValue(json, Day.class);

        // Verify all fields identical
        assertThat(deserialized.getDayNumber()).isEqualTo(day.getDayNumber());
        assertThat(deserialized.getLabel()).isEqualTo(day.getLabel());
        assertThat(deserialized.getFocusArea()).isEqualTo(day.getFocusArea());
        assertThat(deserialized.getModality()).isEqualTo(day.getModality());
        assertThat(deserialized.getMethodologySource()).isEqualTo(day.getMethodologySource());
        assertThat(deserialized.getWarmUp()).isEqualTo(day.getWarmUp());
        assertThat(deserialized.getSections()).isEqualTo(day.getSections());
        assertThat(deserialized.getCoolDown()).isEqualTo(day.getCoolDown());
        assertThat(deserialized).isEqualTo(day);
    }

    // ── Property 2: Conditional required field validation for copied_day ─────

    /**
     * Property 2: Conditional required field validation for copied_day.
     *
     * For any day assignment request with type="copied_day" where any of sourceProgramId,
     * sourceWeekNumber, or sourceDayNumber is null or missing, the endpoint SHALL reject
     * the request with HTTP 400 Bad Request.
     *
     * **Validates: Requirements 1.2, 1.3**
     */
    @Property(tries = 100)
    void missingCopiedDayFieldsRejected(
            @ForAll("copiedDayWithMissingFields") DayAssignmentRequest dayRequest) {

        VaultController controller = createMockedController();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("test-user", null, "ROLE_USER"));

        try {
            CreateManualProgramRequest request = new CreateManualProgramRequest(
                    "Test Program", List.of(dayRequest));

            MockHttpServletRequest httpRequest = new MockHttpServletRequest("POST", "/api/v1/vault/programs");
            ResponseEntity<?> response = controller.createManualProgram(request, httpRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ── Property 3: Non-existent source program is rejected ──────────────────

    /**
     * Property 3: Non-existent source program is rejected.
     *
     * For any UUID that does not exist in the authenticated user's vault when used as
     * sourceProgramId in a copied_day assignment, the service SHALL reject the request
     * by throwing SourceProgramNotFoundException.
     *
     * **Validates: Requirements 1.4, 3.1, 3.2**
     */
    @Property(tries = 100)
    void nonExistentSourceProgramRejected(@ForAll("randomUUID") UUID sourceProgramId) {
        VaultProgramRepository mockRepo = Mockito.mock(VaultProgramRepository.class);
        UploadParser mockParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(mockRepo, mockParser, objectMapper);

        String ownerUserId = "test-user";

        // Mock: source program not found
        when(mockRepo.findByIdAndOwner(eq(sourceProgramId), eq(ownerUserId)))
                .thenReturn(Optional.empty());

        DayAssignment copiedDay = new DayAssignment(
                1, DayAssignmentType.COPIED_DAY, null, null,
                null, sourceProgramId, 1, 1);

        CreateManualProgramCommand command = new CreateManualProgramCommand(
                "Test Program", ownerUserId, List.of(copiedDay));

        assertThatThrownBy(() -> service.createManualProgram(command))
                .isInstanceOf(SourceProgramNotFoundException.class)
                .hasMessageContaining(sourceProgramId.toString());
    }

    // ── Property 4: Non-existent week/day combination is rejected ────────────

    /**
     * Property 4: Non-existent week/day combination is rejected.
     *
     * For any source program with a known structure and any (sourceWeekNumber, sourceDayNumber)
     * pair that does not correspond to an actual week and day within that program's structure,
     * the service SHALL throw SourceDayNotFoundException.
     *
     * **Validates: Requirements 1.5, 3.3**
     */
    @Property(tries = 100)
    void nonExistentWeekDayRejected(
            @ForAll("programWithInvalidDayRef") ProgramWithInvalidDayRef testCase) {

        VaultProgramRepository mockRepo = Mockito.mock(VaultProgramRepository.class);
        UploadParser mockParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(mockRepo, mockParser, objectMapper);

        String ownerUserId = "test-user";
        UUID sourceProgramId = UUID.randomUUID();

        VaultProgram vaultProgram = new VaultProgram(
                sourceProgramId, testCase.program, ownerUserId,
                ContentSource.UPLOADED, Instant.now(), Instant.now());

        when(mockRepo.findByIdAndOwner(eq(sourceProgramId), eq(ownerUserId)))
                .thenReturn(Optional.of(vaultProgram));

        DayAssignment copiedDay = new DayAssignment(
                1, DayAssignmentType.COPIED_DAY, null, null,
                null, sourceProgramId, testCase.invalidWeekNumber, testCase.invalidDayNumber);

        CreateManualProgramCommand command = new CreateManualProgramCommand(
                "Test Program", ownerUserId, List.of(copiedDay));

        assertThatThrownBy(() -> service.createManualProgram(command))
                .isInstanceOf(SourceDayNotFoundException.class);
    }

    // ── Property 5: Non-positive week/day numbers are rejected ───────────────

    /**
     * Property 5: Non-positive week/day numbers are rejected.
     *
     * For any sourceWeekNumber or sourceDayNumber value less than 1 (zero or negative)
     * in a copied_day assignment, the endpoint SHALL reject the request with HTTP 400 Bad Request.
     *
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 100)
    void nonPositiveWeekDayNumbersRejected(
            @ForAll("copiedDayWithNonPositiveNumbers") DayAssignmentRequest dayRequest) {

        VaultController controller = createMockedController();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("test-user", null, "ROLE_USER"));

        try {
            CreateManualProgramRequest request = new CreateManualProgramRequest(
                    "Test Program", List.of(dayRequest));

            MockHttpServletRequest httpRequest = new MockHttpServletRequest("POST", "/api/v1/vault/programs");
            ResponseEntity<?> response = controller.createManualProgram(request, httpRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
            ErrorResponse error = (ErrorResponse) response.getBody();
            assertThat(error.message()).contains("positive integer");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ── Property 6: Unrecognised assignment type is rejected ─────────────────

    /**
     * Property 6: Unrecognised assignment type is rejected.
     *
     * For any string value for the type field that is not "activity" or "copied_day",
     * the endpoint SHALL reject the request with HTTP 400 Bad Request listing the valid types.
     *
     * **Validates: Requirements 8.3**
     */
    @Property(tries = 100)
    void unrecognisedTypeRejected(@ForAll("invalidTypeStrings") String invalidType) {

        VaultController controller = createMockedController();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("test-user", null, "ROLE_USER"));

        try {
            DayAssignmentRequest dayRequest = new DayAssignmentRequest(
                    1, invalidType, null, null, null, null, null);

            CreateManualProgramRequest request = new CreateManualProgramRequest(
                    "Test Program", List.of(dayRequest));

            MockHttpServletRequest httpRequest = new MockHttpServletRequest("POST", "/api/v1/vault/programs");
            ResponseEntity<?> response = controller.createManualProgram(request, httpRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
            ErrorResponse error = (ErrorResponse) response.getBody();
            assertThat(error.message()).contains("Valid types are: activity, copied_day");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ── Property 7: Browse days returns correct structure ────────────────────

    /**
     * Property 7: Browse days returns correct structure.
     *
     * For any vault program with an arbitrary number of weeks and days, calling getProgramDays
     * SHALL return a list where every day in the program appears exactly once with correct
     * week number, day number, label, and focus area.
     *
     * **Validates: Requirements 2.1, 2.2**
     */
    @Property(tries = 100)
    void browseDaysReturnsCorrectStructure(@ForAll("programWithDays") Program program) {

        VaultProgramRepository mockRepo = Mockito.mock(VaultProgramRepository.class);
        UploadParser mockParser = Mockito.mock(UploadParser.class);
        VaultService service = new VaultService(mockRepo, mockParser, objectMapper);

        String ownerUserId = "test-user";
        UUID programId = UUID.randomUUID();

        VaultProgram vaultProgram = new VaultProgram(
                programId, program, ownerUserId,
                ContentSource.UPLOADED, Instant.now(), Instant.now());

        when(mockRepo.findByIdAndOwner(eq(programId), eq(ownerUserId)))
                .thenReturn(Optional.of(vaultProgram));

        List<DaySummary> result = service.getProgramDays(programId, ownerUserId);

        // Collect all expected days from the program structure
        List<DaySummary> expected = program.getWeeks().stream()
                .flatMap(week -> week.getDays().stream()
                        .map(day -> new DaySummary(
                                week.getWeekNumber(), day.getDayNumber(),
                                day.getLabel(), day.getFocusArea())))
                .toList();

        // Every day appears exactly once
        assertThat(result).hasSameSizeAs(expected);
        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Arbitraries
    // ══════════════════════════════════════════════════════════════════════════

    @Provide
    Arbitrary<Day> arbitraryDay() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 7),             // dayNumber
                arbitraryNullableString(),                         // label
                arbitraryNullableString(),                         // focusArea
                Arbitraries.of(Modality.values()),                // modality
                arbitraryWarmCoolEntries(),                        // warmUp
                arbitrarySections(),                               // sections
                arbitraryWarmCoolEntries(),                        // coolDown
                arbitraryNullableString()                          // methodologySource
        ).as(Day::new);
    }

    @Provide
    Arbitrary<DayAssignmentRequest> copiedDayWithMissingFields() {
        // Generate a copied_day request where at least one of the required fields is null/blank
        return Arbitraries.integers().between(1, 7).flatMap(dayNumber ->
                Arbitraries.integers().between(1, 7).flatMap(scenario -> {
                    // scenario determines which field(s) to null out
                    String sourceProgramId = (scenario == 1 || scenario == 4 || scenario == 5 || scenario == 7)
                            ? null : UUID.randomUUID().toString();
                    Integer sourceWeekNumber = (scenario == 2 || scenario == 4 || scenario == 6 || scenario == 7)
                            ? null : 1;
                    Integer sourceDayNumber = (scenario == 3 || scenario == 5 || scenario == 6 || scenario == 7)
                            ? null : 1;

                    return Arbitraries.just(new DayAssignmentRequest(
                            dayNumber, "copied_day", null, null,
                            sourceProgramId, sourceWeekNumber, sourceDayNumber));
                })
        );
    }

    @Provide
    Arbitrary<UUID> randomUUID() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<ProgramWithInvalidDayRef> programWithInvalidDayRef() {
        // Generate a program with known structure, then reference a week/day that doesn't exist
        return Arbitraries.integers().between(1, 5).flatMap(numWeeks ->
                Arbitraries.integers().between(1, 7).flatMap(numDaysPerWeek -> {
                    // Build the program with known structure
                    List<Week> weeks = IntStream.rangeClosed(1, numWeeks)
                            .mapToObj(w -> {
                                List<Day> days = IntStream.rangeClosed(1, numDaysPerWeek)
                                        .mapToObj(d -> new Day(d, "Day " + d, "Focus " + d,
                                                Modality.STRENGTH, List.of(), List.of(), List.of(), null))
                                        .collect(Collectors.toList());
                                return new Week(w, days);
                            })
                            .collect(Collectors.toList());

                    Program program = new Program("Test Program", numWeeks, "Strength",
                            List.of("Barbell"), weeks);

                    // Generate invalid week/day combinations (outside structure bounds)
                    return Arbitraries.oneOf(
                            // Week number too high
                            Arbitraries.integers().between(numWeeks + 1, numWeeks + 10)
                                    .map(invalidWeek -> new ProgramWithInvalidDayRef(program, invalidWeek, 1)),
                            // Day number too high
                            Arbitraries.integers().between(numDaysPerWeek + 1, numDaysPerWeek + 10)
                                    .map(invalidDay -> new ProgramWithInvalidDayRef(program, 1, invalidDay)),
                            // Both week and day too high
                            Combinators.combine(
                                    Arbitraries.integers().between(numWeeks + 1, numWeeks + 5),
                                    Arbitraries.integers().between(numDaysPerWeek + 1, numDaysPerWeek + 5)
                            ).as((invalidWeek, invalidDay) -> new ProgramWithInvalidDayRef(program, invalidWeek, invalidDay))
                    );
                })
        );
    }

    @Provide
    Arbitrary<DayAssignmentRequest> copiedDayWithNonPositiveNumbers() {
        // Generate copied_day requests where sourceWeekNumber or sourceDayNumber <= 0
        return Arbitraries.integers().between(1, 7).flatMap(dayNumber ->
                Arbitraries.oneOf(
                        // sourceWeekNumber is non-positive
                        Arbitraries.integers().lessOrEqual(0)
                                .map(nonPositive -> new DayAssignmentRequest(
                                        dayNumber, "copied_day", null, null,
                                        UUID.randomUUID().toString(), nonPositive, 1)),
                        // sourceDayNumber is non-positive
                        Arbitraries.integers().lessOrEqual(0)
                                .map(nonPositive -> new DayAssignmentRequest(
                                        dayNumber, "copied_day", null, null,
                                        UUID.randomUUID().toString(), 1, nonPositive)),
                        // Both non-positive
                        Combinators.combine(
                                Arbitraries.integers().lessOrEqual(0),
                                Arbitraries.integers().lessOrEqual(0)
                        ).as((w, d) -> new DayAssignmentRequest(
                                dayNumber, "copied_day", null, null,
                                UUID.randomUUID().toString(), w, d))
                )
        );
    }

    @Provide
    Arbitrary<String> invalidTypeStrings() {
        // Generate arbitrary strings that are NOT "activity" or "copied_day" (case-insensitive)
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                .filter(s -> !s.equalsIgnoreCase("activity") && !s.equalsIgnoreCase("copied_day"));
    }

    @Provide
    Arbitrary<Program> programWithDays() {
        return Arbitraries.integers().between(1, 5).flatMap(numWeeks ->
                Arbitraries.integers().between(1, 7).flatMap(numDaysPerWeek -> {
                    Arbitrary<List<Week>> weeksArb = Arbitraries.just(
                            IntStream.rangeClosed(1, numWeeks)
                                    .mapToObj(w -> {
                                        List<Day> days = IntStream.rangeClosed(1, numDaysPerWeek)
                                                .mapToObj(d -> new Day(d,
                                                        "Day " + w + "-" + d,
                                                        "Focus " + w + "-" + d,
                                                        Modality.STRENGTH,
                                                        List.of(), List.of(), List.of(), null))
                                                .collect(Collectors.toList());
                                        return new Week(w, days);
                                    })
                                    .collect(Collectors.toList())
                    );

                    return weeksArb.map(weeks -> new Program(
                            "Test Program", numWeeks, "Strength", List.of("Barbell"), weeks));
                })
        );
    }

    // ── Helper Arbitraries ───────────────────────────────────────────────────

    private Arbitrary<String> arbitraryNullableString() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30)
        );
    }

    private Arbitrary<List<WarmCoolEntry>> arbitraryWarmCoolEntries() {
        Arbitrary<WarmCoolEntry> entry = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30)
        ).as(WarmCoolEntry::new);

        return entry.list().ofMinSize(0).ofMaxSize(4);
    }

    private Arbitrary<List<Section>> arbitrarySections() {
        return arbitrarySection().list().ofMinSize(0).ofMaxSize(3);
    }

    private Arbitrary<Section> arbitrarySection() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),       // name
                Arbitraries.of(SectionType.values()),                                // sectionType
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(15),       // format
                Arbitraries.oneOf(Arbitraries.just(null),                            // timeCap (nullable)
                        Arbitraries.integers().between(1, 60).map(Integer::valueOf)),
                arbitraryExercises()                                                  // exercises
        ).as(Section::new);
    }

    private Arbitrary<List<Exercise>> arbitraryExercises() {
        return arbitraryExercise().list().ofMinSize(1).ofMaxSize(5);
    }

    private Arbitrary<Exercise> arbitraryExercise() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),               // name
                Arbitraries.oneOf(Arbitraries.just(null),                                    // modalityType (nullable)
                        Arbitraries.of(ModalityType.values()).map(m -> m)),
                Arbitraries.integers().between(1, 10),                                      // sets
                Arbitraries.strings().numeric().ofMinLength(1).ofMaxLength(5),              // reps
                Arbitraries.oneOf(Arbitraries.just(null),                                    // weight (nullable)
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)),
                Arbitraries.oneOf(Arbitraries.just(null),                                    // restSeconds (nullable)
                        Arbitraries.integers().between(30, 300).map(Integer::valueOf)),
                Arbitraries.oneOf(Arbitraries.just(null),                                    // notes (nullable)
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30))
        ).as(Exercise::new);
    }

    // ── Helper: Create mocked VaultController ────────────────────────────────

    private VaultController createMockedController() {
        return new VaultController(
                Mockito.mock(ListProgramsUseCase.class),
                Mockito.mock(GetProgramUseCase.class),
                Mockito.mock(UpdateProgramUseCase.class),
                Mockito.mock(DeleteProgramUseCase.class),
                Mockito.mock(CopyProgramUseCase.class),
                Mockito.mock(SearchProgramsUseCase.class),
                Mockito.mock(CreateManualProgramUseCase.class),
                Mockito.mock(GetProgramDaysUseCase.class)
        );
    }

    // ── Helper record for Property 4 ─────────────────────────────────────────

    record ProgramWithInvalidDayRef(Program program, int invalidWeekNumber, int invalidDayNumber) {}
}
