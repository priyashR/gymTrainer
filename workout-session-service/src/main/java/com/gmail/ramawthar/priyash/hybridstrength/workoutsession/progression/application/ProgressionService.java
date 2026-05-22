package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.application;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.AccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.EnrollmentNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.AdvanceDayUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.EnrollProgramUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.GetEnrollmentUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.SkipDayUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.outbound.EnrollmentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service orchestrating program enrollment and day progression.
 * Implements all progression-related inbound ports.
 */
@Service
@Transactional
public class ProgressionService implements EnrollProgramUseCase, AdvanceDayUseCase,
        SkipDayUseCase, GetEnrollmentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProgressionService.class);

    private final EnrollmentRepository enrollmentRepository;

    public ProgressionService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public ProgramEnrollment enrollProgram(String userId, UUID programId, String programName,
                                           int totalWeeks, int totalDaysPerWeek) {
        log.info("Enrolling user={} in program={} ({})", userId, programId, programName);

        // End any existing active enrollment by marking it as REPLACED
        Optional<ProgramEnrollment> existingEnrollment = enrollmentRepository.findActiveByUserId(userId);
        if (existingEnrollment.isPresent()) {
            ProgramEnrollment existing = existingEnrollment.get();
            existing.markReplaced();
            enrollmentRepository.save(existing);
            log.info("Replaced existing enrollment: id={}, program={}",
                    existing.getId(), existing.getProgramId());
        }

        // Create new enrollment at week 1, day 1
        Instant now = Instant.now();
        UUID enrollmentId = UUID.randomUUID();

        ProgramEnrollment enrollment = ProgramEnrollment.create(
                enrollmentId, userId, programId, programName,
                totalWeeks, totalDaysPerWeek, now
        );

        ProgramEnrollment saved = enrollmentRepository.save(enrollment);

        log.info("Enrollment created: id={}, program={}, totalWeeks={}, daysPerWeek={}",
                enrollmentId, programId, totalWeeks, totalDaysPerWeek);
        return saved;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProgramEnrollment advanceDay(UUID enrollmentId, String userId) {
        ProgramEnrollment enrollment = loadEnrollment(enrollmentId);
        verifyOwnership(enrollment, userId);

        enrollment.advanceDay();

        ProgramEnrollment saved = enrollmentRepository.save(enrollment);

        log.info("Day advanced: enrollment={}, now at week={}, day={}, status={}",
                enrollmentId, saved.getCurrentWeek(), saved.getCurrentDay(), saved.getStatus());
        return saved;
    }

    @Override
    public ProgramEnrollment skipDay(UUID enrollmentId, String userId) {
        ProgramEnrollment enrollment = loadEnrollment(enrollmentId);
        verifyOwnership(enrollment, userId);

        Instant now = Instant.now();
        enrollment.skipDay(now);

        ProgramEnrollment saved = enrollmentRepository.save(enrollment);

        log.info("Day skipped: enrollment={}, now at week={}, day={}, status={}",
                enrollmentId, saved.getCurrentWeek(), saved.getCurrentDay(), saved.getStatus());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProgramEnrollment> getActiveEnrollment(String userId) {
        return enrollmentRepository.findActiveByUserId(userId);
    }

    // --- Private helpers ---

    private ProgramEnrollment loadEnrollment(UUID enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));
    }

    private void verifyOwnership(ProgramEnrollment enrollment, String userId) {
        if (!enrollment.getUserId().equals(userId)) {
            throw new AccessDeniedException();
        }
    }
}
