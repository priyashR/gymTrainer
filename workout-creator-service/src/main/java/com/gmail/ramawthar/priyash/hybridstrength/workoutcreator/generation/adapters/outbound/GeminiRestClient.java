package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.GeminiUnavailableException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.ports.outbound.GeminiClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Outbound adapter that calls the Google Gemini REST API.
 * <p>
 * Uses Spring {@link RestClient} configured in {@code GeminiConfig} with a
 * 10-second read timeout (matching the Resilience4j time limiter config).
 * The Resilience4j circuit breaker protects against Gemini outages — when
 * the circuit is open, the fallback throws {@link GeminiUnavailableException}
 * which maps to 502 Bad Gateway.
 * <p>
 * Note: {@code @TimeLimiter} is not used because it requires a
 * {@code CompletableFuture} return type. Timeout enforcement is handled by
 * the RestClient's read timeout (configured in {@code GeminiConfig}) and the
 * circuit breaker's slow-call tracking.
 */
@Component
public class GeminiRestClient implements GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiRestClient.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiRestClient(RestClient geminiRestClient,
                            @Value("${gemini.api-key}") String apiKey,
                            @Value("${gemini.model}") String model) {
        this.restClient = geminiRestClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    @CircuitBreaker(name = "gemini", fallbackMethod = "geminiUnavailable")
    public String generate(String prompt) {
        log.info("Calling Gemini model={}", model);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        GeminiResponse response = restClient.post()
                .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new GeminiUnavailableException("Gemini returned an empty response");
        }

        String text = response.candidates().get(0).content().parts().get(0).text();
        log.info("Gemini response received, length={}", text.length());
        return text;
    }

    @SuppressWarnings("unused")
    private String geminiUnavailable(String prompt, Throwable t) {
        log.error("Gemini unavailable: {}", t.getMessage());
        throw new GeminiUnavailableException(
                "AI generation service is currently unavailable. Please try again later.", t);
    }

    /**
     * Minimal record hierarchy to deserialise the Gemini generateContent response.
     * Only the fields we need are mapped — everything else is ignored by Jackson.
     */
    record GeminiResponse(List<Candidate> candidates) {}
    record Candidate(Content content) {}
    record Content(List<Part> parts) {}
    record Part(String text) {}
}
