package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response representation of a single exercise's completion state and performance data.
 */
public record ExerciseLogResponse(
        int exerciseIndex,
        String exerciseName,
        Integer restSeconds,
        boolean completed,
        Instant completedAt,
        List<SetLogResponse> setLogs
) {
}
