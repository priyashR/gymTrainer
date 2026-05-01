package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

/**
 * Checked exception thrown when the {@link WorkoutParser} cannot parse
 * Gemini text into a valid domain object.
 * <p>
 * The message is always human-readable and suitable for inclusion in API responses.
 */
public class ParsingException extends Exception {

    public ParsingException(String message) {
        super(message);
    }

    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
