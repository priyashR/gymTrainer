package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.GeminiUnavailableException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.application.GenerationService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.ports.outbound.GeminiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GenerationService}.
 * <p>
 * Mocks {@link GeminiClient} to test the orchestration logic: prompt building,
 * Gemini call, sanitisation, parsing, and result assembly.
 * <p>
 * Naming convention: MethodName_StateUnderTest_ExpectedBehaviour
 * No Spring context — plain Java instantiation with Mockito mocks.
 *
 * @see GenerationService
 * Requirements: 1.1, 5.1, 5.2, 5.3, 5.4
 */
class GenerationServiceTest {

    private GeminiClient geminiClient;
    private GenerationService generationService;

    @BeforeEach
    void setUp() {
        geminiClient = Mockito.mock(GeminiClient.class);
        generationService = new GenerationService(geminiClient);
    }

    // ---- Helper methods for building valid Gemini response text ----

    private static String validDayWorkoutResponse() {
        return """
                WORKOUT: Strength Day
                DESCRIPTION: Heavy compound lifts
                TRAINING_STYLE: STRENGTH
                --- SECTION: Main Lifts [TYPE: STRENGTH] ---
                - Back Squat | Sets: 5 | Reps: 5 | Weight: 225 lbs | Rest: 180s
                - Bench Press | Sets: 4 | Reps: 6 | Weight: 185 lbs | Rest: 120s""";
    }

    private static String validWeekProgramResponse() {
        StringBuilder sb = new StringBuilder();
        sb.append("PROGRAM: Weekly Plan\n");
        sb.append("DESCRIPTION: A full week of training\n");
        sb.append("SCOPE: WEEK\n");
        sb.append("TRAINING_STYLES: STRENGTH, CROSSFIT\n");
        for (int day = 1; day <= 7; day++) {
            sb.append("\n=== DAY ").append(day).append(" ===\n");
            sb.append("WORKOUT: Day ").append(day).append(" Session\n");
            sb.append("DESCRIPTION: Training for day ").append(day).append("\n");
            sb.append("TRAINING_STYLE: STRENGTH\n");
            sb.append("--- SECTION: Block A [TYPE: STRENGTH] ---\n");
            sb.append("- Squat | Sets: 3 | Reps: 8 | Weight: 200 lbs | Rest: 90s\n");
        }
        return sb.toString().stripTrailing();
    }

    private static String validFourWeekProgramResponse() {
        StringBuilder sb = new StringBuilder();
        sb.append("PROGRAM: Monthly Plan\n");
        sb.append("DESCRIPTION: A four week hypertrophy block\n");
        sb.append("SCOPE: FOUR_WEEK\n");
        sb.append("TRAINING_STYLES: HYPERTROPHY\n");
        for (int day = 1; day <= 28; day++) {
            sb.append("\n=== DAY ").append(day).append(" ===\n");
            sb.append("WORKOUT: Day ").append(day).append(" Session\n");
            sb.append("DESCRIPTION: Hypertrophy day ").append(day).append("\n");
            sb.append("TRAINING_STYLE: HYPERTROPHY\n");
            sb.append("--- SECTION: Volume Work [TYPE: STRENGTH] ---\n");
            sb.append("- Leg Press | Sets: 4 | Reps: 10 | Weight: 300 lbs | Rest: 60s\n");
        }
        return sb.toString().stripTrailing();
    }

    private static GenerationCommand dayCommand() {
        return new GenerationCommand(
                UUID.randomUUID(),
                "Heavy strength session",
                GenerationScope.DAY,
                List.of(TrainingStyle.STRENGTH)
        );
    }

    private static GenerationCommand weekCommand() {
        return new GenerationCommand(
                UUID.randomUUID(),
                "Full week of training",
                GenerationScope.WEEK,
                List.of(TrainingStyle.STRENGTH, TrainingStyle.CROSSFIT)
        );
    }

    private static GenerationCommand fourWeekCommand() {
        return new GenerationCommand(
                UUID.randomUUID(),
                "Monthly hypertrophy block",
                GenerationScope.FOUR_WEEK,
                List.of(TrainingStyle.HYPERTROPHY)
        );
    }


    // ---- Success path tests ----

    @Nested
    @DisplayName("generate — success path")
    class SuccessPath {

        @Test
        @DisplayName("generate_DayScope_ReturnsWorkoutAndNullProgram")
        void generate_DayScope_ReturnsWorkoutAndNullProgram() {
            when(geminiClient.generate(anyString())).thenReturn(validDayWorkoutResponse());

            GenerationResult result = generationService.generate(dayCommand());

            assertNotNull(result.getRawGeminiResponse());
            assertNotNull(result.getWorkout());
            assertNull(result.getProgram());
            assertNull(result.getParsingError());
            assertEquals("Strength Day", result.getWorkout().getName());
            assertEquals(TrainingStyle.STRENGTH, result.getWorkout().getTrainingStyle());
            verify(geminiClient, times(1)).generate(anyString());
        }

        @Test
        @DisplayName("generate_WeekScope_ReturnsProgramAndNullWorkout")
        void generate_WeekScope_ReturnsProgramAndNullWorkout() {
            when(geminiClient.generate(anyString())).thenReturn(validWeekProgramResponse());

            GenerationResult result = generationService.generate(weekCommand());

            assertNotNull(result.getRawGeminiResponse());
            assertNull(result.getWorkout());
            assertNotNull(result.getProgram());
            assertNull(result.getParsingError());
            assertEquals("Weekly Plan", result.getProgram().getName());
            assertEquals(GenerationScope.WEEK, result.getProgram().getScope());
            assertEquals(7, result.getProgram().getWorkouts().size());
            verify(geminiClient, times(1)).generate(anyString());
        }

        @Test
        @DisplayName("generate_FourWeekScope_ReturnsProgramAndNullWorkout")
        void generate_FourWeekScope_ReturnsProgramAndNullWorkout() {
            when(geminiClient.generate(anyString())).thenReturn(validFourWeekProgramResponse());

            GenerationResult result = generationService.generate(fourWeekCommand());

            assertNotNull(result.getRawGeminiResponse());
            assertNull(result.getWorkout());
            assertNotNull(result.getProgram());
            assertNull(result.getParsingError());
            assertEquals("Monthly Plan", result.getProgram().getName());
            assertEquals(GenerationScope.FOUR_WEEK, result.getProgram().getScope());
            assertEquals(28, result.getProgram().getWorkouts().size());
        }

        @Test
        @DisplayName("generate_SuccessfulParse_RawResponseMatchesGeminiOutput")
        void generate_SuccessfulParse_RawResponseMatchesGeminiOutput() {
            String geminiOutput = validDayWorkoutResponse();
            when(geminiClient.generate(anyString())).thenReturn(geminiOutput);

            GenerationResult result = generationService.generate(dayCommand());

            assertEquals(geminiOutput, result.getRawGeminiResponse());
        }

        @Test
        @DisplayName("generate_DayScopeSuccess_ResultHasCorrectStructure")
        void generate_DayScopeSuccess_ResultHasCorrectStructure() {
            when(geminiClient.generate(anyString())).thenReturn(validDayWorkoutResponse());

            GenerationResult result = generationService.generate(dayCommand());

            // Req 5.4: success result has raw text, parsed object, null error
            assertNotNull(result.getRawGeminiResponse());
            assertNotNull(result.getWorkout());
            assertNull(result.getProgram());
            assertNull(result.getParsingError());
        }
    }

    // ---- Parse failure path tests ----

    @Nested
    @DisplayName("generate — parse failure path")
    class ParseFailurePath {

        @Test
        @DisplayName("generate_UnparseableGeminiResponse_ReturnsRawTextAndError")
        void generate_UnparseableGeminiResponse_ReturnsRawTextAndError() {
            String malformedResponse = "This is not a valid workout format at all.";
            when(geminiClient.generate(anyString())).thenReturn(malformedResponse);

            GenerationResult result = generationService.generate(dayCommand());

            // Req 5.2, 5.3: parse failure returns 200-style result with raw text and error
            assertNotNull(result.getRawGeminiResponse());
            assertEquals(malformedResponse, result.getRawGeminiResponse());
            assertNull(result.getWorkout());
            assertNull(result.getProgram());
            assertNotNull(result.getParsingError());
            assertFalse(result.getParsingError().isBlank());
        }

        @Test
        @DisplayName("generate_UnparseableProgramResponse_ReturnsRawTextAndError")
        void generate_UnparseableProgramResponse_ReturnsRawTextAndError() {
            String malformedResponse = "Some random text that is not a program.";
            when(geminiClient.generate(anyString())).thenReturn(malformedResponse);

            GenerationResult result = generationService.generate(weekCommand());

            assertNotNull(result.getRawGeminiResponse());
            assertEquals(malformedResponse, result.getRawGeminiResponse());
            assertNull(result.getWorkout());
            assertNull(result.getProgram());
            assertNotNull(result.getParsingError());
        }

        @Test
        @DisplayName("generate_PartiallyMalformedResponse_ReturnsErrorNotException")
        void generate_PartiallyMalformedResponse_ReturnsErrorNotException() {
            // A response that starts correctly but has a broken section
            String partialResponse = """
                    WORKOUT: Almost Valid
                    DESCRIPTION: This workout has issues
                    TRAINING_STYLE: STRENGTH
                    --- SECTION: Bad Section [TYPE: UNKNOWN_TYPE] ---
                    - Exercise | Sets: 3 | Reps: 10""";
            when(geminiClient.generate(anyString())).thenReturn(partialResponse);

            // Should NOT throw — should return graceful failure result
            GenerationResult result = generationService.generate(dayCommand());

            assertNotNull(result.getRawGeminiResponse());
            assertNull(result.getWorkout());
            assertNull(result.getProgram());
            assertNotNull(result.getParsingError());
        }

        @Test
        @DisplayName("generate_ParseFailure_DoesNotReturnServerError")
        void generate_ParseFailure_DoesNotReturnServerError() {
            when(geminiClient.generate(anyString())).thenReturn("garbage input");

            // Req 5.3: parse failure must NOT throw — it returns a result with error info
            assertDoesNotThrow(() -> generationService.generate(dayCommand()));
        }
    }

    // ---- Gemini error path tests ----

    @Nested
    @DisplayName("generate — Gemini error path")
    class GeminiErrorPath {

        @Test
        @DisplayName("generate_GeminiUnavailable_PropagatesException")
        void generate_GeminiUnavailable_PropagatesException() {
            when(geminiClient.generate(anyString()))
                    .thenThrow(new GeminiUnavailableException("Gemini service is unavailable"));

            assertThrows(GeminiUnavailableException.class,
                    () -> generationService.generate(dayCommand()));
        }

        @Test
        @DisplayName("generate_GeminiTimeout_PropagatesException")
        void generate_GeminiTimeout_PropagatesException() {
            when(geminiClient.generate(anyString()))
                    .thenThrow(new GeminiUnavailableException("Gemini timed out", new RuntimeException("timeout")));

            GeminiUnavailableException ex = assertThrows(GeminiUnavailableException.class,
                    () -> generationService.generate(dayCommand()));
            assertTrue(ex.getMessage().contains("timed out"));
        }

        @Test
        @DisplayName("generate_GeminiCircuitBreakerOpen_PropagatesException")
        void generate_GeminiCircuitBreakerOpen_PropagatesException() {
            when(geminiClient.generate(anyString()))
                    .thenThrow(new GeminiUnavailableException("Circuit breaker is open"));

            GeminiUnavailableException ex = assertThrows(GeminiUnavailableException.class,
                    () -> generationService.generate(dayCommand()));
            assertNotNull(ex.getMessage());
        }
    }

    // ---- Null / invalid input tests ----

    @Nested
    @DisplayName("generate — null input handling")
    class NullInputHandling {

        @Test
        @DisplayName("generate_NullCommand_ThrowsNullPointerException")
        void generate_NullCommand_ThrowsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> generationService.generate(null));
            verify(geminiClient, never()).generate(anyString());
        }
    }

    // ---- Constructor tests ----

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("constructor_NullGeminiClient_ThrowsNullPointerException")
        void constructor_NullGeminiClient_ThrowsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> new GenerationService(null));
        }
    }

    // ---- Prompt delegation tests ----

    @Nested
    @DisplayName("generate — prompt delegation")
    class PromptDelegation {

        @Test
        @DisplayName("generate_ValidCommand_CallsGeminiClientExactlyOnce")
        void generate_ValidCommand_CallsGeminiClientExactlyOnce() {
            when(geminiClient.generate(anyString())).thenReturn(validDayWorkoutResponse());

            generationService.generate(dayCommand());

            verify(geminiClient, times(1)).generate(anyString());
        }

        @Test
        @DisplayName("generate_ValidCommand_PassesNonEmptyPromptToGemini")
        void generate_ValidCommand_PassesNonEmptyPromptToGemini() {
            when(geminiClient.generate(anyString())).thenReturn(validDayWorkoutResponse());

            generationService.generate(dayCommand());

            verify(geminiClient).generate(argThat(prompt ->
                    prompt != null && !prompt.isBlank()));
        }
    }

    // ---- Sanitisation tests ----

    @Nested
    @DisplayName("generate — content sanitisation")
    class ContentSanitisation {

        @Test
        @DisplayName("generate_ResponseWithHtmlTags_SanitisesBeforeParsing")
        void generate_ResponseWithHtmlTags_SanitisesBeforeParsing() {
            // Inject HTML tags into an otherwise valid response — sanitiser should strip them
            String responseWithHtml = """
                    WORKOUT: <b>Strength Day</b>
                    DESCRIPTION: Heavy compound lifts
                    TRAINING_STYLE: STRENGTH
                    --- SECTION: Main Lifts [TYPE: STRENGTH] ---
                    - Back Squat | Sets: 5 | Reps: 5 | Weight: 225 lbs | Rest: 180s""";
            when(geminiClient.generate(anyString())).thenReturn(responseWithHtml);

            GenerationResult result = generationService.generate(dayCommand());

            // Raw response preserves original (unsanitised) text
            assertEquals(responseWithHtml, result.getRawGeminiResponse());
            // The workout name should have HTML stripped by the sanitiser
            if (result.getWorkout() != null) {
                assertFalse(result.getWorkout().getName().contains("<b>"));
                assertFalse(result.getWorkout().getName().contains("</b>"));
            }
        }
    }
}
