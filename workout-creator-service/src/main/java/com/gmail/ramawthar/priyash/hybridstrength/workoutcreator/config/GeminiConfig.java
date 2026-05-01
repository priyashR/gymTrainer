package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for the Gemini AI API client.
 * <p>
 * Provides a pre-configured {@link RestClient} bean targeting the Gemini base URL
 * with a 10-second read timeout matching the Resilience4j time limiter configuration.
 * The circuit breaker is configured in application.yml and applied via annotations
 * on the outbound adapter ({@code GeminiRestClient}).
 */
@Configuration
public class GeminiConfig {

    @Value("${gemini.base-url}")
    private String baseUrl;

    @Bean
    RestClient geminiRestClient() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(10));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
