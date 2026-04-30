package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.ContentSanitiser;

import net.jqwik.api.*;

import java.util.regex.Pattern;

/**
 * Property-based tests for ContentSanitiser.
 * <p>
 * Feature: workout-creator-service-mvp1, Property 6: Sanitiser removes unsafe content
 * Validates: Requirements 2.3
 */
class ContentSanitiserPropertyTest {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>", Pattern.DOTALL);
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    // ── Providers ─────────────────────────────────────────────────────

    @Provide
    Arbitrary<String> plainTextStrings() {
        return Arbitraries.of(
                "Back Squat 5x5 at 225 lbs",
                "AMRAP 12 minutes: 10 thrusters, 15 box jumps",
                "Rest 90 seconds between sets",
                "Week 1 Day 3 — Hypertrophy Upper",
                "3 rounds for time: 400m run, 21 KB swings",
                "Tabata: 20s on / 10s off x 8 rounds",
                "Notes: focus on tempo 3-1-2-0",
                "");
    }

    @Provide
    Arbitrary<String> htmlTagStrings() {
        return Arbitraries.of(
                "<script>alert('xss')</script>",
                "<img onerror=alert(1) src=x>",
                "<div class=\"malicious\">",
                "<iframe src=\"evil.com\"></iframe>",
                "<style>body{display:none}</style>",
                "<a href=\"javascript:void(0)\">click</a>",
                "<b>bold</b>",
                "<p>paragraph</p>",
                "</div>",
                "<br/>",
                "<svg onload=alert(1)>");
    }

    @Provide
    Arbitrary<String> controlCharStrings() {
        return Arbitraries.of(
                "\u0000", "\u0001", "\u0007", "\u0008",
                "\u000B", "\u000C", "\u000E", "\u001F", "\u007F",
                "\u0000\u0001\u0007hidden");
    }

    @Provide
    Arbitrary<String> stringsWithEmbeddedTags() {
        return Combinators.combine(plainTextStrings(), htmlTagStrings(), plainTextStrings())
                .as((before, tag, after) -> before + tag + after);
    }

    @Provide
    Arbitrary<String> stringsWithEmbeddedControlChars() {
        return Combinators.combine(plainTextStrings(), controlCharStrings(), plainTextStrings())
                .as((before, ctrl, after) -> before + ctrl + after);
    }

    // ── Property: sanitised output contains no HTML tags ──────────────

    // Feature: workout-creator-service-mvp1, Property 6: Sanitiser removes unsafe content
    @Property(tries = 100)
    void sanitisedOutputContainsNoHtmlTags(
            @ForAll("stringsWithEmbeddedTags") String input) {

        String result = ContentSanitiser.sanitise(input);

        assert !HTML_TAG_PATTERN.matcher(result).find()
                : "Sanitised output must not contain HTML tags, but got: " + result;
    }

    // ── Property: sanitised output contains no control characters ─────

    // Feature: workout-creator-service-mvp1, Property 6: Sanitiser removes unsafe content
    @Property(tries = 100)
    void sanitisedOutputContainsNoControlCharacters(
            @ForAll("stringsWithEmbeddedControlChars") String input) {

        String result = ContentSanitiser.sanitise(input);

        assert !CONTROL_CHAR_PATTERN.matcher(result).find()
                : "Sanitised output must not contain control characters, but got: " + result;
    }

    // ── Property: plain text is preserved ─────────────────────────────

    // Feature: workout-creator-service-mvp1, Property 6: Sanitiser removes unsafe content
    @Property(tries = 100)
    void plainTextContentIsPreserved(
            @ForAll("plainTextStrings") String plainText) {

        String result = ContentSanitiser.sanitise(plainText);

        assert result.equals(plainText)
                : "Plain text must be preserved. Expected: '" + plainText + "', got: '" + result + "'";
    }

    // ── Property: plain text around tags is preserved ─────────────────

    // Feature: workout-creator-service-mvp1, Property 6: Sanitiser removes unsafe content
    @Property(tries = 100)
    void plainTextAroundTagsIsPreserved(
            @ForAll("plainTextStrings") String before,
            @ForAll("htmlTagStrings") String tag,
            @ForAll("plainTextStrings") String after) {

        String input = before + tag + after;
        String result = ContentSanitiser.sanitise(input);

        assert result.contains(before)
                : "Text before tag must be preserved. Expected to contain: '" + before + "', got: '" + result + "'";
        assert result.contains(after)
                : "Text after tag must be preserved. Expected to contain: '" + after + "', got: '" + result + "'";
    }

    // ── Property: null input returns empty string ─────────────────────

    // Feature: workout-creator-service-mvp1, Property 6: Sanitiser removes unsafe content
    @Property(tries = 1)
    void nullInputReturnsEmptyString() {
        String result = ContentSanitiser.sanitise(null);

        assert result.isEmpty()
                : "Null input must return empty string, got: '" + result + "'";
    }

    // ── Property: whitespace characters are preserved ─────────────────

    // Feature: workout-creator-service-mvp1, Property 6: Sanitiser removes unsafe content
    @Property(tries = 100)
    void tabsNewlinesAndCarriageReturnsArePreserved(
            @ForAll("plainTextStrings") String text) {

        String input = text + "\t\n\r" + text;
        String result = ContentSanitiser.sanitise(input);

        assert result.contains("\t") : "Tab must be preserved";
        assert result.contains("\n") : "Newline must be preserved";
        assert result.contains("\r") : "Carriage return must be preserved";
    }

    // ── Property: script tags with content are fully removed ──────────

    // Feature: workout-creator-service-mvp1, Property 6: Sanitiser removes unsafe content
    @Property(tries = 100)
    void scriptTagsAreFullyRemoved(
            @ForAll("plainTextStrings") String payload,
            @ForAll("plainTextStrings") String surrounding) {

        String input = surrounding + "<script>" + payload + "</script>" + surrounding;
        String result = ContentSanitiser.sanitise(input);

        assert !result.contains("<script>") : "Opening script tag must be removed";
        assert !result.contains("</script>") : "Closing script tag must be removed";
    }

    // ── Property: idempotence — sanitising twice equals sanitising once

    // Feature: workout-creator-service-mvp1, Property 6: Sanitiser removes unsafe content
    @Property(tries = 100)
    void sanitisingIsIdempotent(
            @ForAll("stringsWithEmbeddedTags") String input) {

        String once = ContentSanitiser.sanitise(input);
        String twice = ContentSanitiser.sanitise(once);

        assert once.equals(twice)
                : "Sanitising must be idempotent. First pass: '" + once + "', second pass: '" + twice + "'";
    }
}
