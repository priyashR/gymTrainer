package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.WorkoutFetcher;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * REST-based implementation of the {@link WorkoutFetcher} outbound port.
 * Calls the Workout Creator Service at {@code /api/v1/vault/programs/{id}} to fetch
 * program definitions. Wrapped with a Resilience4j circuit breaker (5s timeout).
 */
@Component
public class RestWorkoutFetcher implements WorkoutFetcher {

    private static final Logger log = LoggerFactory.getLogger(RestWorkoutFetcher.class);

    private final RestClient restClient;

    public RestWorkoutFetcher(
            @Value("${workout-creator-service.base-url:http://localhost:8082}") String baseUrl,
            RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    @CircuitBreaker(name = "workoutFetcher", fallbackMethod = "fetchProgramFallback")
    public String fetchProgram(UUID programId, String jwt) {
        log.debug("Fetching program definition for programId={}", programId);

        return restClient.get()
                .uri("/api/v1/vault/programs/{id}", programId)
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    log.error("Workout Creator Service returned error status {} for programId={}",
                            response.getStatusCode(), programId);
                    throw new WorkoutFetchException(
                            "Workout Creator Service returned " + response.getStatusCode());
                })
                .body(String.class);
    }

    /**
     * Fallback when the circuit breaker is open or the call fails.
     * Throws a {@link WorkoutFetchException} that maps to HTTP 502.
     */
    @SuppressWarnings("unused")
    private String fetchProgramFallback(UUID programId, String jwt, Throwable throwable) {
        log.error("Circuit breaker fallback triggered for programId={}: {}", programId, throwable.getMessage());
        throw new WorkoutFetchException("Unable to retrieve workout definition");
    }
}
