package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Configuration for the Gemini AI API client.
 * <p>
 * Provides a pre-configured {@link RestClient} bean targeting the Gemini base URL.
 * Resilience4j circuit breaker and time limiter are configured in application.yml
 * and applied via annotations on the outbound adapter ({@code GeminiRestClient}).
 */
@Configuration
public class GeminiConfig {

    @Value("${gemini.base-url}")
    private String baseUrl;

    @Bean
    RestClient geminiRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
