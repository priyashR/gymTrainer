package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseRecommendation;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for retrieving exercise recommendations for a session.
 * Enforces ownership — only the session owner can access their recommendations.
 */
public interface GetRecommendationsUseCase {

    /**
     * Retrieves exercise recommendations for all sections in the session.
     *
     * @param sessionId the session to retrieve recommendations for
     * @param userId    the authenticated user's ID (for ownership check)
     * @return ordered list of exercise recommendations grouped by section then exercise index
     * @throws com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.SessionNotFoundException if session not found
     */
    List<ExerciseRecommendation> getRecommendations(UUID sessionId, String userId);

    /**
     * Retrieves exercise recommendations for a specific section in the session.
     *
     * @param sessionId    the session to retrieve recommendations for
     * @param userId       the authenticated user's ID (for ownership check)
     * @param sectionIndex the target section index (0-based)
     * @return ordered list of exercise recommendations for the specified section
     * @throws com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.SessionNotFoundException if session not found
     */
    List<ExerciseRecommendation> getRecommendationsForSection(UUID sessionId, String userId, int sectionIndex);
}
