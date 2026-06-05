package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.AdvanceSectionRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.CompleteExerciseRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.LogCrossFitScoreRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.LogSetRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.SessionResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.StartSessionRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.EndSessionUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.GetSessionUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.LogCrossFitScoreUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.LogSetUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.PauseSessionUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.StartSessionUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.UpdateSessionUseCase;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for workout session lifecycle operations.
 * All endpoints require JWT authentication; the user ID is extracted from the security context.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    private final StartSessionUseCase startSessionUseCase;
    private final GetSessionUseCase getSessionUseCase;
    private final UpdateSessionUseCase updateSessionUseCase;
    private final PauseSessionUseCase pauseSessionUseCase;
    private final EndSessionUseCase endSessionUseCase;
    private final LogSetUseCase logSetUseCase;
    private final LogCrossFitScoreUseCase logCrossFitScoreUseCase;

    public SessionController(StartSessionUseCase startSessionUseCase,
                             GetSessionUseCase getSessionUseCase,
                             UpdateSessionUseCase updateSessionUseCase,
                             PauseSessionUseCase pauseSessionUseCase,
                             EndSessionUseCase endSessionUseCase,
                             LogSetUseCase logSetUseCase,
                             LogCrossFitScoreUseCase logCrossFitScoreUseCase) {
        this.startSessionUseCase = startSessionUseCase;
        this.getSessionUseCase = getSessionUseCase;
        this.updateSessionUseCase = updateSessionUseCase;
        this.pauseSessionUseCase = pauseSessionUseCase;
        this.endSessionUseCase = endSessionUseCase;
        this.logSetUseCase = logSetUseCase;
        this.logCrossFitScoreUseCase = logCrossFitScoreUseCase;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> startSession(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody StartSessionRequest request) {
        log.info("POST /api/v1/sessions — user={}, program={}", userId, request.programId());

        UUID sessionId = startSessionUseCase.startSession(
                userId.toString(),
                request.programId(),
                request.weekNumber(),
                request.dayNumber(),
                request.standalone()
        );

        Session session = getSessionUseCase.getSession(sessionId, userId.toString());
        SessionResponse response = SessionResponse.from(session);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        Session session = getSessionUseCase.getSession(id, userId.toString());
        return ResponseEntity.ok(SessionResponse.from(session));
    }

    @GetMapping("/active")
    public ResponseEntity<SessionResponse> getActiveSession(@AuthenticationPrincipal UUID userId) {
        Optional<Session> activeSession = getSessionUseCase.getActiveSession(userId.toString());

        return activeSession
                .map(session -> ResponseEntity.ok(SessionResponse.from(session)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}/exercises")
    public ResponseEntity<SessionResponse> completeExercise(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CompleteExerciseRequest request) {
        Session session = updateSessionUseCase.completeExercise(
                id, userId.toString(), request.sectionIndex(), request.exerciseIndex());
        return ResponseEntity.ok(SessionResponse.from(session));
    }

    @PatchMapping("/{id}/section")
    public ResponseEntity<SessionResponse> advanceSection(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody AdvanceSectionRequest request) {
        Session session = updateSessionUseCase.advanceSection(
                id, userId.toString(), request.targetSectionIndex());
        return ResponseEntity.ok(SessionResponse.from(session));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<SessionResponse> pauseSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        Session session = pauseSessionUseCase.pauseSession(id, userId.toString());
        return ResponseEntity.ok(SessionResponse.from(session));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<SessionResponse> resumeSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        Session session = pauseSessionUseCase.resumeSession(id, userId.toString());
        return ResponseEntity.ok(SessionResponse.from(session));
    }

    @PostMapping("/{id}/sets")
    public ResponseEntity<SessionResponse> logSet(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody LogSetRequest request) {
        Session session = logSetUseCase.logSet(
                id, userId.toString(), request.sectionIndex(), request.exerciseIndex(),
                request.weight(), request.repetitions(), request.rpe());
        return ResponseEntity.ok(SessionResponse.from(session));
    }

    @PostMapping("/{id}/scores")
    public ResponseEntity<SessionResponse> logCrossFitScore(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody LogCrossFitScoreRequest request) {
        Session session = logCrossFitScoreUseCase.logCrossFitScore(
                id, userId.toString(), request.sectionIndex(),
                request.rounds(), request.additionalReps(), request.totalTimeSeconds());
        return ResponseEntity.ok(SessionResponse.from(session));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<SessionResponse> endSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        Session session = endSessionUseCase.endSession(id, userId.toString());
        return ResponseEntity.ok(SessionResponse.from(session));
    }
}
