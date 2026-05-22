package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.adapters.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.adapters.inbound.dto.EnrollRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.adapters.inbound.dto.EnrollmentResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.EnrollProgramUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.GetEnrollmentUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.SkipDayUseCase;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for program enrollment and day progression operations.
 * All endpoints require JWT authentication; the user ID is extracted from the security context.
 */
@RestController
@RequestMapping("/api/v1/enrollments")
public class ProgressionController {

    private static final Logger log = LoggerFactory.getLogger(ProgressionController.class);

    private final EnrollProgramUseCase enrollProgramUseCase;
    private final GetEnrollmentUseCase getEnrollmentUseCase;
    private final SkipDayUseCase skipDayUseCase;

    public ProgressionController(EnrollProgramUseCase enrollProgramUseCase,
                                 GetEnrollmentUseCase getEnrollmentUseCase,
                                 SkipDayUseCase skipDayUseCase) {
        this.enrollProgramUseCase = enrollProgramUseCase;
        this.getEnrollmentUseCase = getEnrollmentUseCase;
        this.skipDayUseCase = skipDayUseCase;
    }

    @PostMapping
    public ResponseEntity<EnrollmentResponse> enrollProgram(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody EnrollRequest request) {
        log.info("POST /api/v1/enrollments — user={}, program={}", userId, request.programId());

        ProgramEnrollment enrollment = enrollProgramUseCase.enrollProgram(
                userId.toString(),
                request.programId(),
                request.programName(),
                request.totalWeeks(),
                request.totalDaysPerWeek()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(EnrollmentResponse.from(enrollment));
    }

    @GetMapping("/active")
    public ResponseEntity<EnrollmentResponse> getActiveEnrollment(@AuthenticationPrincipal UUID userId) {
        Optional<ProgramEnrollment> enrollment = getEnrollmentUseCase.getActiveEnrollment(userId.toString());

        return enrollment
                .map(e -> ResponseEntity.ok(EnrollmentResponse.from(e)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/skip")
    public ResponseEntity<EnrollmentResponse> skipDay(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        ProgramEnrollment enrollment = skipDayUseCase.skipDay(id, userId.toString());
        return ResponseEntity.ok(EnrollmentResponse.from(enrollment));
    }
}
