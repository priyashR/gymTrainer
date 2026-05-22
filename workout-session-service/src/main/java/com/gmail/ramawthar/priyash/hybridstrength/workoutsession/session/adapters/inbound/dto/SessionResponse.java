package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response representation of a workout session's state.
 */
public record SessionResponse(
        UUID id,
        String status,
        int currentSectionIndex,
        List<SectionProgressResponse> sectionProgresses,
        Object workoutSnapshot,
        Instant startedAt,
        Instant pausedAt,
        Instant completedAt
) {

    /**
     * Maps a domain Session to its API response representation.
     */
    public static SessionResponse from(Session session) {
        List<SectionProgressResponse> progresses = session.getSectionProgresses().stream()
                .map(SessionResponse::mapSectionProgress)
                .toList();

        return new SessionResponse(
                session.getId(),
                session.getStatus().name(),
                session.getCurrentSectionIndex(),
                progresses,
                session.getWorkoutSnapshot(),
                session.getStartedAt(),
                session.getPausedAt(),
                session.getCompletedAt()
        );
    }

    private static SectionProgressResponse mapSectionProgress(SectionProgress sp) {
        List<ExerciseLogResponse> logs = sp.getExerciseLogs().stream()
                .map(SessionResponse::mapExerciseLog)
                .toList();

        return new SectionProgressResponse(
                sp.getSectionIndex(),
                sp.getSectionName(),
                sp.getSectionType().name(),
                logs,
                sp.isCompleted()
        );
    }

    private static ExerciseLogResponse mapExerciseLog(ExerciseLog el) {
        return new ExerciseLogResponse(
                el.getExerciseIndex(),
                el.getExerciseName(),
                el.getRestSeconds(),
                el.isCompleted(),
                el.getCompletedAt()
        );
    }
}
