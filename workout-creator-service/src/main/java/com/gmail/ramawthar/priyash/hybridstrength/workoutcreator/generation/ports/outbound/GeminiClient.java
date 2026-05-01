package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.ports.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.GeminiUnavailableException;

/**
 * Outbound port for calling the external Gemini AI service.
 * <p>
 * Implementations are responsible for HTTP communication, circuit breaking,
 * and timeout handling. Callers should expect {@link GeminiUnavailableException}
 * when Gemini is unreachable, times out, or the circuit breaker is open.
 */
public interface GeminiClient {

    /**
     * Sends a structured prompt to Gemini and returns the raw text response.
     *
     * @param prompt the structured prompt string; must not be null or blank
     * @return the raw text response from Gemini
     * @throws GeminiUnavailableException if Gemini is unreachable, times out,
     *                                     or the circuit breaker is open
     */
    String generate(String prompt);
}
