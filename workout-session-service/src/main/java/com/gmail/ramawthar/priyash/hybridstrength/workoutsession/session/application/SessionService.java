package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.application;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.event.SessionCompletedEvent;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.AccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.SessionNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.AdvanceDayUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound.GetEnrollmentUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.EndSessionUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.GetSessionUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.PauseSessionUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.StartSessionUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.UpdateSessionUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionEventPublisher;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionNotifier;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionRepository;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.WorkoutFetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service orchestrating workout session lifecycle.
 * Implements all session-related inbound ports.
 */
@Service
public class SessionService implements StartSessionUseCase, GetSessionUseCase,
        UpdateSessionUseCase, PauseSessionUseCase, EndSessionUseCase {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;
    private final WorkoutFetcher workoutFetcher;
    private final SessionEventPublisher sessionEventPublisher;
    private final SessionNotifier sessionNotifier;
    private final AdvanceDayUseCase advanceDayUseCase;
    private final GetEnrollmentUseCase getEnrollmentUseCase;
    private final ObjectMapper objectMapper;

    public SessionService(SessionRepository sessionRepository,
                          WorkoutFetcher workoutFetcher,
                          SessionEventPublisher sessionEventPublisher,
                          SessionNotifier sessionNotifier,
                          AdvanceDayUseCase advanceDayUseCase,
                          GetEnrollmentUseCase getEnrollmentUseCase,
                          ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.workoutFetcher = workoutFetcher;
        this.sessionEventPublisher = sessionEventPublisher;
        this.sessionNotifier = sessionNotifier;
        this.advanceDayUseCase = advanceDayUseCase;
        this.getEnrollmentUseCase = getEnrollmentUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public UUID startSession(String userId, UUID programId, int weekNumber, int dayNumber, boolean standalone) {
        log.info("Starting session for user={}, program={}, week={}, day={}, standalone={}",
                userId, programId, weekNumber, dayNumber, standalone);

        // Fetch the program definition from the Workout Creator Service
        String jwt = extractCurrentJwt();
        String programJson = workoutFetcher.fetchProgram(programId, jwt);

        // Parse the workout snapshot to build section progresses
        List<SectionProgress> sectionProgresses = parseSectionProgresses(programJson, weekNumber, dayNumber);

        // Determine enrollment ID if this is a program session
        UUID enrollmentId = null;
        if (!standalone) {
            Optional<ProgramEnrollment> activeEnrollment = getEnrollmentUseCase.getActiveEnrollment(userId);
            if (activeEnrollment.isPresent()) {
                enrollmentId = activeEnrollment.get().getId();
            }
        }

        Instant now = Instant.now();
        UUID sessionId = UUID.randomUUID();

        Session session = Session.start(
                sessionId,
                userId,
                programId,
                enrollmentId,
                weekNumber,
                dayNumber,
                sectionProgresses,
                programJson,
                now
        );

        sessionRepository.save(session);

        log.info("Session started: id={}, sections={}", sessionId, sectionProgresses.size());
        return sessionId;
    }

    @Override
    @Transactional(readOnly = true)
    public Session getSession(UUID sessionId, String userId) {
        Session session = loadSession(sessionId);
        verifyOwnership(session, userId);
        return session;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Session> getActiveSession(String userId) {
        return sessionRepository.findActiveByUserId(userId);
    }

    @Override
    @Transactional
    public Session completeExercise(UUID sessionId, String userId, int sectionIndex, int exerciseIndex) {
        Session session = loadSession(sessionId);
        verifyOwnership(session, userId);

        Instant now = Instant.now();
        session.completeExercise(sectionIndex, exerciseIndex, now);

        Session saved = sessionRepository.save(session);
        sessionNotifier.notifySessionUpdate(saved);

        log.debug("Exercise completed: session={}, section={}, exercise={}",
                sessionId, sectionIndex, exerciseIndex);
        return saved;
    }

    @Override
    @Transactional
    public Session advanceSection(UUID sessionId, String userId, int targetSectionIndex) {
        Session session = loadSession(sessionId);
        verifyOwnership(session, userId);

        session.advanceSection(targetSectionIndex);

        Session saved = sessionRepository.save(session);
        sessionNotifier.notifySessionUpdate(saved);

        log.debug("Section advanced: session={}, targetSection={}", sessionId, targetSectionIndex);
        return saved;
    }

    @Override
    @Transactional
    public Session pauseSession(UUID sessionId, String userId) {
        Session session = loadSession(sessionId);
        verifyOwnership(session, userId);

        Instant now = Instant.now();
        session.pause(now);

        Session saved = sessionRepository.save(session);
        sessionNotifier.notifySessionUpdate(saved);

        log.info("Session paused: id={}", sessionId);
        return saved;
    }

    @Override
    @Transactional
    public Session resumeSession(UUID sessionId, String userId) {
        Session session = loadSession(sessionId);
        verifyOwnership(session, userId);

        if (session.getStatus() != SessionStatus.PAUSED) {
            throw new IllegalStateException(
                    "Can only resume a PAUSED session, current status: " + session.getStatus());
        }

        // Reconstitute as IN_PROGRESS — domain doesn't have a resume method,
        // so we rebuild with the same state but IN_PROGRESS status
        Session resumed = new Session.Builder()
                .id(session.getId())
                .userId(session.getUserId())
                .programId(session.getProgramId())
                .enrollmentId(session.getEnrollmentId())
                .weekNumber(session.getWeekNumber())
                .dayNumber(session.getDayNumber())
                .status(SessionStatus.IN_PROGRESS)
                .currentSectionIndex(session.getCurrentSectionIndex())
                .sectionProgresses(session.getSectionProgresses())
                .workoutSnapshot(session.getWorkoutSnapshot())
                .startedAt(session.getStartedAt())
                .pausedAt(null)
                .completedAt(null)
                .lastPersistedAt(Instant.now())
                .build();

        Session saved = sessionRepository.save(resumed);
        sessionNotifier.notifySessionUpdate(saved);

        log.info("Session resumed: id={}", sessionId);
        return saved;
    }

    @Override
    @Transactional
    public Session endSession(UUID sessionId, String userId) {
        Session session = loadSession(sessionId);
        verifyOwnership(session, userId);

        Instant now = Instant.now();
        session.end(now);

        Session saved = sessionRepository.save(session);

        // Publish SessionCompleted event
        boolean standalone = (session.getEnrollmentId() == null);
        SessionCompletedEvent event = new SessionCompletedEvent(
                UUID.randomUUID(),
                now,
                session.getUserId(),
                session.getId(),
                session.getProgramId(),
                session.getWeekNumber(),
                session.getDayNumber(),
                standalone,
                session.getSectionProgresses(),
                session.getStartedAt(),
                now
        );
        sessionEventPublisher.publishSessionCompleted(event);

        // Advance program day pointer if this is a program session (not standalone)
        if (!standalone) {
            try {
                advanceDayUseCase.advanceDay(session.getEnrollmentId(), userId);
                log.info("Program day advanced after session completion: enrollment={}",
                        session.getEnrollmentId());
            } catch (Exception e) {
                // Log but don't fail the session completion — the session is already marked complete
                log.error("Failed to advance program day after session completion: enrollment={}, error={}",
                        session.getEnrollmentId(), e.getMessage(), e);
            }
        }

        sessionNotifier.notifySessionCompleted(saved);

        log.info("Session ended: id={}, standalone={}", sessionId, standalone);
        return saved;
    }

    // --- Private helpers ---

    private Session loadSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    private void verifyOwnership(Session session, String userId) {
        if (!session.getUserId().equals(userId)) {
            throw new AccessDeniedException();
        }
    }

    /**
     * Extracts the raw JWT token from the current security context.
     * The token is stored as credentials by the JwtAuthenticationFilter.
     */
    private String extractCurrentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof String token) {
            return token;
        }
        return null;
    }

    /**
     * Parses the program JSON to extract section progresses for the specified week and day.
     * Falls back to a single section with a single exercise if parsing fails.
     */
    private List<SectionProgress> parseSectionProgresses(String programJson, int weekNumber, int dayNumber) {
        try {
            JsonNode root = objectMapper.readTree(programJson);

            // Navigate to the specific day's workout definition
            JsonNode dayNode = findDayNode(root, weekNumber, dayNumber);
            if (dayNode == null) {
                throw new IllegalArgumentException(
                        "Could not find workout for week " + weekNumber + ", day " + dayNumber);
            }

            JsonNode sectionsNode = dayNode.get("sections");
            if (sectionsNode == null || !sectionsNode.isArray() || sectionsNode.isEmpty()) {
                throw new IllegalArgumentException("Workout day has no sections defined");
            }

            List<SectionProgress> progresses = new ArrayList<>();
            for (int i = 0; i < sectionsNode.size(); i++) {
                JsonNode sectionNode = sectionsNode.get(i);
                String sectionName = sectionNode.has("name") ? sectionNode.get("name").asText() : "Section " + (i + 1);
                SectionType sectionType = parseSectionType(sectionNode);

                JsonNode exercisesNode = sectionNode.get("exercises");
                List<ExerciseLog> exerciseLogs = new ArrayList<>();
                if (exercisesNode != null && exercisesNode.isArray()) {
                    for (int j = 0; j < exercisesNode.size(); j++) {
                        JsonNode exerciseNode = exercisesNode.get(j);
                        String exerciseName = exerciseNode.has("name")
                                ? exerciseNode.get("name").asText()
                                : "Exercise " + (j + 1);
                        Integer restSeconds = parseRestSeconds(exerciseNode);
                        exerciseLogs.add(new ExerciseLog(j, exerciseName, restSeconds));
                    }
                }

                progresses.add(new SectionProgress(i, sectionName, sectionType, exerciseLogs));
            }

            return progresses;
        } catch (Exception e) {
            log.error("Failed to parse workout definition: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid workout definition: " + e.getMessage(), e);
        }
    }

    private JsonNode findDayNode(JsonNode root, int weekNumber, int dayNumber) {
        // Try common program structures:
        // Structure 1: { "weeks": [ { "days": [ {...} ] } ] }
        JsonNode weeksNode = root.get("weeks");
        if (weeksNode != null && weeksNode.isArray() && weeksNode.size() >= weekNumber) {
            JsonNode weekNode = weeksNode.get(weekNumber - 1);
            JsonNode daysNode = weekNode.get("days");
            if (daysNode != null && daysNode.isArray() && daysNode.size() >= dayNumber) {
                return daysNode.get(dayNumber - 1);
            }
        }

        // Structure 2: root is the day itself (single workout)
        if (root.has("sections")) {
            return root;
        }

        return null;
    }

    private SectionType parseSectionType(JsonNode sectionNode) {
        if (sectionNode.has("type")) {
            String type = sectionNode.get("type").asText().toUpperCase();
            try {
                return SectionType.valueOf(type);
            } catch (IllegalArgumentException e) {
                // Default to STRENGTH if unrecognized
                return SectionType.STRENGTH;
            }
        }
        return SectionType.STRENGTH;
    }

    private Integer parseRestSeconds(JsonNode exerciseNode) {
        JsonNode restNode = exerciseNode.get("restSeconds");
        if (restNode != null && restNode.isNumber()) {
            return restNode.asInt();
        }
        JsonNode restIntervalNode = exerciseNode.get("rest_interval_seconds");
        if (restIntervalNode != null && restIntervalNode.isNumber()) {
            return restIntervalNode.asInt();
        }
        return null;
    }
}
