package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.application.ProgressionService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.EnrollmentStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.SkipRecord;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.AdvanceDayUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.GetEnrollmentUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.outbound.EnrollmentRepository;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.application.SessionService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionEventPublisher;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionNotifier;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionRepository;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.WorkoutFetcher;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.jqwik.api.*;
import net.jqwik.api.Combinators;
import net.jqwik.api.Tuple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Property-based tests for Progression domain logic.
 * Tests Properties 6–9 from the design document.
 */
class ProgressionPropertyTest {

    private static final String USER_ID = "user-prop-test";

    // --- Generators ---

    @Provide
    Arbitrary<ProgramEnrollment> nonFinalEnrollments() {
        return Arbitraries.integers().between(1, 12).flatMap(totalWeeks ->
                Arbitraries.integers().between(1, 7).flatMap(totalDaysPerWeek -> {
                    // Generate a position that is NOT the final position
                    // Final position is (totalWeeks, totalDaysPerWeek)
                    // We need to generate (week, day) where it's not the last
                    int totalPositions = totalWeeks * totalDaysPerWeek;
                    if (totalPositions <= 1) {
                        // Only one position, can't be non-final — use at least 2 positions
                        return Arbitraries.just(createEnrollmentAt(1, 1, 2, totalDaysPerWeek));
                    }
                    // Generate a position index from 0 to totalPositions-2 (excluding last)
                    return Arbitraries.integers().between(0, totalPositions - 2).map(posIndex -> {
                        int week = (posIndex / totalDaysPerWeek) + 1;
                        int day = (posIndex % totalDaysPerWeek) + 1;
                        return createEnrollmentAt(week, day, totalWeeks, totalDaysPerWeek);
                    });
                })
        );
    }

    @Provide
    Arbitrary<ProgramEnrollment> anyActiveEnrollments() {
        return Arbitraries.integers().between(1, 12).flatMap(totalWeeks ->
                Arbitraries.integers().between(1, 7).flatMap(totalDaysPerWeek ->
                        Arbitraries.integers().between(1, totalWeeks).flatMap(week ->
                                Arbitraries.integers().between(1, totalDaysPerWeek).map(day ->
                                        createEnrollmentAt(week, day, totalWeeks, totalDaysPerWeek)
                                )
                        )
                )
        );
    }

    private ProgramEnrollment createEnrollmentAt(int week, int day, int totalWeeks, int totalDaysPerWeek) {
        return new ProgramEnrollment.Builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .programId(UUID.randomUUID())
                .programName("Test Program")
                .currentWeek(week)
                .currentDay(day)
                .totalWeeks(totalWeeks)
                .totalDaysPerWeek(totalDaysPerWeek)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .skips(new java.util.ArrayList<>())
                .build();
    }

    // --- Property 6: Day pointer advancement ---

    /**
     * **Validates: Requirements 2.1, 2.7**
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-active-workout, Property 6: Day pointer advancement")
    void dayPointerAdvancement_advancesToCorrectNextPosition(@ForAll("nonFinalEnrollments") ProgramEnrollment enrollment) {
        int weekBefore = enrollment.getCurrentWeek();
        int dayBefore = enrollment.getCurrentDay();
        int totalDaysPerWeek = enrollment.getTotalDaysPerWeek();

        // Calculate expected next position
        int expectedWeek;
        int expectedDay;
        if (dayBefore < totalDaysPerWeek) {
            expectedWeek = weekBefore;
            expectedDay = dayBefore + 1;
        } else {
            expectedWeek = weekBefore + 1;
            expectedDay = 1;
        }

        // Test advanceDay
        enrollment.advanceDay();

        assert enrollment.getCurrentWeek() == expectedWeek :
                "After advanceDay from (" + weekBefore + ", " + dayBefore + "), expected week " + expectedWeek + " but got " + enrollment.getCurrentWeek();
        assert enrollment.getCurrentDay() == expectedDay :
                "After advanceDay from (" + weekBefore + ", " + dayBefore + "), expected day " + expectedDay + " but got " + enrollment.getCurrentDay();

        // Now test that skip produces the same advancement
        // Create a fresh enrollment at the same position
        ProgramEnrollment enrollmentForSkip = createEnrollmentAt(weekBefore, dayBefore,
                enrollment.getTotalWeeks(), totalDaysPerWeek);

        enrollmentForSkip.skipDay(Instant.now());

        assert enrollmentForSkip.getCurrentWeek() == expectedWeek :
                "After skipDay from (" + weekBefore + ", " + dayBefore + "), expected week " + expectedWeek + " but got " + enrollmentForSkip.getCurrentWeek();
        assert enrollmentForSkip.getCurrentDay() == expectedDay :
                "After skipDay from (" + weekBefore + ", " + dayBefore + "), expected day " + expectedDay + " but got " + enrollmentForSkip.getCurrentDay();
    }

    // --- Property 7: Skip records creation ---

    /**
     * Property 7: Skip records creation
     * For any program enrollment at position (week W, day D), skipping should create a
     * SkipRecord with weekNumber = W, dayNumber = D, and a non-null skippedAt timestamp,
     * in addition to advancing the pointer.
     *
     * **Validates: Requirements 2.8**
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-active-workout, Property 7: Skip records creation")
    void skipDay_createsSkipRecordWithCorrectData(@ForAll("anyActiveEnrollments") ProgramEnrollment enrollment) {
        int weekBefore = enrollment.getCurrentWeek();
        int dayBefore = enrollment.getCurrentDay();
        int skipCountBefore = enrollment.getSkips().size();
        Instant skipTime = Instant.now();

        enrollment.skipDay(skipTime);

        // Verify a new SkipRecord was appended
        assert enrollment.getSkips().size() == skipCountBefore + 1 :
                "Skip records count should increase by 1 after skipDay, was " + skipCountBefore
                        + " now " + enrollment.getSkips().size();

        SkipRecord lastSkip = enrollment.getSkips().get(enrollment.getSkips().size() - 1);

        // Verify SkipRecord captures the position at the time of skip
        assert lastSkip.getWeekNumber() == weekBefore :
                "SkipRecord weekNumber should be " + weekBefore + " but got " + lastSkip.getWeekNumber();
        assert lastSkip.getDayNumber() == dayBefore :
                "SkipRecord dayNumber should be " + dayBefore + " but got " + lastSkip.getDayNumber();
        assert lastSkip.getSkippedAt() != null :
                "SkipRecord skippedAt should be non-null";
        assert lastSkip.getSkippedAt().equals(skipTime) :
                "SkipRecord skippedAt should equal the provided timestamp";
    }

    // --- Property 8: Standalone session enrollment invariant ---

    /**
     * Property 8: Standalone session enrollment invariant
     *
     * For any user with an active program enrollment at position (week W, day D),
     * starting and completing a standalone session (enrollmentId = null) should NOT
     * modify the enrollment's currentWeek, currentDay, or status fields.
     *
     * This test validates at the service level that SessionService.endSession() does
     * NOT call AdvanceDayUseCase when the session is standalone.
     *
     * **Validates: Requirements 2.4, 3.2, 3.5**
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-active-workout, Property 8: Standalone session enrollment invariant")
    void standaloneSession_doesNotModifyEnrollment(@ForAll("anyActiveEnrollments") ProgramEnrollment enrollment) {
        // Capture enrollment state before standalone session
        int weekBefore = enrollment.getCurrentWeek();
        int dayBefore = enrollment.getCurrentDay();
        EnrollmentStatus statusBefore = enrollment.getStatus();

        // Create a standalone session (enrollmentId = null signals standalone)
        List<ExerciseLog> exerciseLogs = new ArrayList<>();
        exerciseLogs.add(new ExerciseLog(0, "Bench Press"));
        List<SectionProgress> sections = new ArrayList<>();
        sections.add(new SectionProgress(0, "Strength", SectionType.STRENGTH, exerciseLogs));

        Session standaloneSession = Session.start(
                UUID.randomUUID(),
                enrollment.getUserId(),
                UUID.randomUUID(),  // some program ID
                null,               // enrollmentId = null → standalone
                1, 1,
                sections,
                "{\"sections\":[]}",
                Instant.now()
        );

        // Set up mocked outbound ports
        SessionRepository sessionRepository = mock(SessionRepository.class);
        WorkoutFetcher workoutFetcher = mock(WorkoutFetcher.class);
        SessionEventPublisher eventPublisher = mock(SessionEventPublisher.class);
        SessionNotifier sessionNotifier = mock(SessionNotifier.class);
        AdvanceDayUseCase advanceDayUseCase = mock(AdvanceDayUseCase.class);
        GetEnrollmentUseCase getEnrollmentUseCase = mock(GetEnrollmentUseCase.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(sessionRepository.findById(standaloneSession.getId()))
                .thenReturn(Optional.of(standaloneSession));
        when(sessionRepository.save(any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SessionService sessionService = new SessionService(
                sessionRepository, workoutFetcher, eventPublisher,
                sessionNotifier, advanceDayUseCase, getEnrollmentUseCase, objectMapper
        );

        // End the standalone session
        sessionService.endSession(standaloneSession.getId(), enrollment.getUserId());

        // Assert: AdvanceDayUseCase was NEVER called (standalone sessions don't advance enrollment)
        verify(advanceDayUseCase, never()).advanceDay(any(), any());

        // Assert: enrollment state remains unchanged
        assert enrollment.getCurrentWeek() == weekBefore :
                "Enrollment currentWeek should not change for standalone session, was " + weekBefore
                        + " but got " + enrollment.getCurrentWeek();
        assert enrollment.getCurrentDay() == dayBefore :
                "Enrollment currentDay should not change for standalone session, was " + dayBefore
                        + " but got " + enrollment.getCurrentDay();
        assert enrollment.getStatus() == statusBefore :
                "Enrollment status should not change for standalone session, was " + statusBefore
                        + " but got " + enrollment.getStatus();
    }

    // --- Property 9: Program enrollment replacement ---

    /**
     * Property 9: Program enrollment replacement
     *
     * For any user (with or without an existing active enrollment), enrolling in a new
     * program should result in exactly one enrollment with status ACTIVE for that user,
     * positioned at (week 1, day 1). If a previous active enrollment existed, it should
     * now have status REPLACED.
     *
     * This test exercises the ProgressionService.enrollProgram() method with a mocked
     * repository, generating users with 0 or 1 active enrollment.
     *
     * **Validates: Requirements 3.4, 3.7**
     */
    @Property(tries = 100)
    @Label("Feature: workout-session-service-active-workout, Property 9: Program enrollment replacement")
    void enrollmentReplacement_resultsInSingleActiveAtDayOne(
            @ForAll("enrollmentScenarios") Optional<ProgramEnrollment> existingEnrollment,
            @ForAll("programParams") ProgramParams newProgramParams) {

        // Track all saved enrollments to verify final state
        List<ProgramEnrollment> savedEnrollments = new ArrayList<>();

        // Set up mocked repository
        EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
        when(enrollmentRepository.findActiveByUserId(USER_ID)).thenReturn(existingEnrollment);
        when(enrollmentRepository.save(any(ProgramEnrollment.class)))
                .thenAnswer(invocation -> {
                    ProgramEnrollment saved = invocation.getArgument(0);
                    savedEnrollments.add(saved);
                    return saved;
                });

        ProgressionService progressionService = new ProgressionService(enrollmentRepository);

        // Execute enrollment
        ProgramEnrollment result = progressionService.enrollProgram(
                USER_ID,
                newProgramParams.programId(),
                newProgramParams.programName(),
                newProgramParams.totalWeeks(),
                newProgramParams.totalDaysPerWeek()
        );

        // Assert: the returned enrollment is ACTIVE at (week 1, day 1)
        assert result.getStatus() == EnrollmentStatus.ACTIVE :
                "New enrollment should be ACTIVE but got " + result.getStatus();
        assert result.getCurrentWeek() == 1 :
                "New enrollment should start at week 1 but got " + result.getCurrentWeek();
        assert result.getCurrentDay() == 1 :
                "New enrollment should start at day 1 but got " + result.getCurrentDay();
        assert result.getProgramId().equals(newProgramParams.programId()) :
                "New enrollment programId should match the requested program";
        assert result.getUserId().equals(USER_ID) :
                "New enrollment userId should match the requesting user";

        // Assert: if a previous enrollment existed, it is now REPLACED
        if (existingEnrollment.isPresent()) {
            ProgramEnrollment previous = existingEnrollment.get();
            assert previous.getStatus() == EnrollmentStatus.REPLACED :
                    "Previous enrollment should be REPLACED but got " + previous.getStatus();

            // Verify the previous enrollment was saved (persisted the REPLACED status)
            assert savedEnrollments.stream()
                    .anyMatch(e -> e.getId().equals(previous.getId()) && e.getStatus() == EnrollmentStatus.REPLACED) :
                    "Previous enrollment with REPLACED status should have been persisted";
        }

        // Assert: exactly one ACTIVE enrollment was saved (the new one)
        long activeCount = savedEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .count();
        assert activeCount == 1 :
                "Exactly one ACTIVE enrollment should be saved, but found " + activeCount;
    }

    // --- Generators for Property 9 ---

    @Provide
    Arbitrary<Optional<ProgramEnrollment>> enrollmentScenarios() {
        Arbitrary<ProgramEnrollment> existingEnrollment = Arbitraries.integers().between(1, 12).flatMap(totalWeeks ->
                Arbitraries.integers().between(1, 7).flatMap(totalDaysPerWeek ->
                        Arbitraries.integers().between(1, totalWeeks).flatMap(week ->
                                Arbitraries.integers().between(1, totalDaysPerWeek).map(day ->
                                        createEnrollmentAt(week, day, totalWeeks, totalDaysPerWeek)
                                )
                        )
                )
        );

        // 50% chance of no existing enrollment, 50% chance of an active one
        return Arbitraries.frequencyOf(
                Tuple.of(1, Arbitraries.just(Optional.<ProgramEnrollment>empty())),
                Tuple.of(1, existingEnrollment.map(Optional::of))
        );
    }

    @Provide
    Arbitrary<ProgramParams> programParams() {
        return Combinators.combine(
                Arbitraries.create(UUID::randomUUID),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30),
                Arbitraries.integers().between(1, 16),
                Arbitraries.integers().between(1, 7)
        ).as(ProgramParams::new);
    }

    /**
     * Record holding parameters for a new program enrollment.
     */
    record ProgramParams(UUID programId, String programName, int totalWeeks, int totalDaysPerWeek) {}
}
