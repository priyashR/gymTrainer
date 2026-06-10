package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.RecommendationsResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseRecommendation;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.GetRecommendationsUseCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for exercise recommendation retrieval.
 * Delegates to the GetRecommendationsUseCase and maps domain objects to the response DTO.
 * Exception handling is centralised in GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class RecommendationController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationController.class);

    private final GetRecommendationsUseCase getRecommendationsUseCase;

    public RecommendationController(GetRecommendationsUseCase getRecommendationsUseCase) {
        this.getRecommendationsUseCase = getRecommendationsUseCase;
    }

    @GetMapping("/{sessionId}/recommendations")
    public ResponseEntity<RecommendationsResponse> getRecommendations(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UUID userId) {

        log.info("GET /api/v1/sessions/{}/recommendations — user={}", sessionId, userId);

        List<ExerciseRecommendation> recommendations =
                getRecommendationsUseCase.getRecommendations(sessionId, userId.toString());

        RecommendationsResponse response = RecommendationsResponse.from(recommendations);
        return ResponseEntity.ok(response);
    }
}
