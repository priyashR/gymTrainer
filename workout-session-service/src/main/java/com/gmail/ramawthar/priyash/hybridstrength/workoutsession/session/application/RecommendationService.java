package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.application;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.AccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.SessionNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseRecommendation;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.RecommendationEngine;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.GetRecommendationsUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Application service that orchestrates exercise recommendation retrieval.
 * Loads the session, verifies ownership, and delegates computation to the RecommendationEngine.
 */
@Service
public class RecommendationService implements GetRecommendationsUseCase {

    private final SessionRepository sessionRepository;
    private final RecommendationEngine recommendationEngine;

    public RecommendationService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
        this.recommendationEngine = new RecommendationEngine();
    }

    @Override
    public List<ExerciseRecommendation> getRecommendations(UUID sessionId, String userId) {
        Session session = loadSession(sessionId);
        verifyOwnership(session, userId);

        return recommendationEngine.computeAll(
                session.getWorkoutSnapshot(),
                session.getWeekNumber(),
                session.getDayNumber()
        );
    }

    @Override
    public List<ExerciseRecommendation> getRecommendationsForSection(UUID sessionId, String userId, int sectionIndex) {
        Session session = loadSession(sessionId);
        verifyOwnership(session, userId);

        return recommendationEngine.computeForSection(
                session.getWorkoutSnapshot(),
                session.getWeekNumber(),
                session.getDayNumber(),
                sectionIndex
        );
    }

    private Session loadSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    private void verifyOwnership(Session session, String userId) {
        if (!session.getUserId().equals(userId)) {
            throw new AccessDeniedException();
        }
    }
}
