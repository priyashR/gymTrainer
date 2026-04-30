package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

import java.util.regex.Pattern;

/**
 * Strips HTML tags, script injections, and control characters from Gemini output.
 * Preserves all plain-text content that is not part of a tag.
 * <p>
 * Pure string manipulation — no framework imports.
 */
public final class ContentSanitiser {

    /**
     * Matches HTML/XML tags including self-closing tags and tags with attributes.
     * Handles multi-line tags via {@link Pattern#DOTALL}.
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>", Pattern.DOTALL);

    /**
     * Matches ASCII control characters (U+0000–U+001F and U+007F) except for
     * common whitespace characters: tab (U+0009), newline (U+000A), and
     * carriage return (U+000D), which are preserved.
     */
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    private ContentSanitiser() {
        // Utility class — not instantiable
    }

    /**
     * Sanitises the given input by stripping HTML tags and control characters.
     *
     * @param input the raw text to sanitise; may be null
     * @return the sanitised text, or an empty string if input is null
     */
    public static String sanitise(String input) {
        if (input == null) {
            return "";
        }

        // 1. Strip all HTML/XML tags
        String result = HTML_TAG_PATTERN.matcher(input).replaceAll("");

        // 2. Strip control characters (preserving tab, newline, carriage return)
        result = CONTROL_CHAR_PATTERN.matcher(result).replaceAll("");

        return result;
    }
}
