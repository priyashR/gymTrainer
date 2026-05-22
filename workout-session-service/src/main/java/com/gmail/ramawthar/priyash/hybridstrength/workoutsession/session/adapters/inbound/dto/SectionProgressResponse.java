package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto;

import java.util.List;

/**
 * Response representation of a section's progress within a session.
 */
public record SectionProgressResponse(
        int sectionIndex,
        String sectionName,
        String sectionType,
        List<ExerciseLogResponse> exerciseLogs,
        boolean completed
) {
}
