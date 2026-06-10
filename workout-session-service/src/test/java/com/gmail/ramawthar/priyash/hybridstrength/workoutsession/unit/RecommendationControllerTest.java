package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.AccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.SessionNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.RecommendationController;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.inbound.dto.RecommendationsResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseRecommendation;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SnapshotParseException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound.GetRecommendationsUseCase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecommendationController.
 * Direct invocation with mocked GetRecommendationsUseCase — no Spring context.
 * Exception-to-HTTP-status mapping is verified at the GlobalExceptionHandler level;
 * here we verify correct delegation, response shape, and exception propagation.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private GetRecommendationsUseCase getRecommendationsUseCase;

    private RecommendationController controller;

    @BeforeEach
    void setUp() {
        controller = new RecommendationController(getRecommendationsUseCase);
    }

    @Nested
    @DisplayName("getRecommendations")
    class GetRecommendations {

        @Test
        @DisplayName("getRecommendations_ValidSessionAndOwner_Returns200WithCorrectResponseShape")
        void getRecommendations_ValidSessionAndOwner_Returns200WithCorrectResponseShape() {
            List<ExerciseRecommendation> recommendations = List.of(
                    new ExerciseRecommendation(0, 0, "80kg", "8-10", 4),
                    new ExerciseRecommendation(0, 1, "60kg", "10", 3),
                    new ExerciseRecommendation(1, 0, "20kg", "12-15", 3)
            );
            when(getRecommendationsUseCase.getRecommendations(SESSION_ID, USER_ID.toString()))
                    .thenReturn(recommendations);

            ResponseEntity<RecommendationsResponse> response =
                    controller.getRecommendations(SESSION_ID, USER_ID);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            RecommendationsResponse body = response.getBody();
            assertEquals(2, body.sections().size());

            // Section 0 has 2 exercises
            RecommendationsResponse.SectionRecommendations section0 = body.sections().get(0);
            assertEquals(0, section0.sectionIndex());
            assertEquals(2, section0.exercises().size());

            RecommendationsResponse.ExerciseRecommendationDto ex0 = section0.exercises().get(0);
            assertEquals(0, ex0.exerciseIndex());
            assertEquals("80kg", ex0.prescribedWeight());
            assertEquals("8-10", ex0.prescribedReps());
            assertEquals(4, ex0.prescribedSets());

            RecommendationsResponse.ExerciseRecommendationDto ex1 = section0.exercises().get(1);
            assertEquals(1, ex1.exerciseIndex());
            assertEquals("60kg", ex1.prescribedWeight());
            assertEquals("10", ex1.prescribedReps());
            assertEquals(3, ex1.prescribedSets());

            // Section 1 has 1 exercise
            RecommendationsResponse.SectionRecommendations section1 = body.sections().get(1);
            assertEquals(1, section1.sectionIndex());
            assertEquals(1, section1.exercises().size());

            verify(getRecommendationsUseCase).getRecommendations(SESSION_ID, USER_ID.toString());
        }

        @Test
        @DisplayName("getRecommendations_EmptyRecommendations_Returns200WithEmptySections")
        void getRecommendations_EmptyRecommendations_Returns200WithEmptySections() {
            when(getRecommendationsUseCase.getRecommendations(SESSION_ID, USER_ID.toString()))
                    .thenReturn(List.of());

            ResponseEntity<RecommendationsResponse> response =
                    controller.getRecommendations(SESSION_ID, USER_ID);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().sections().isEmpty());

            verify(getRecommendationsUseCase).getRecommendations(SESSION_ID, USER_ID.toString());
        }

        @Test
        @DisplayName("getRecommendations_SessionNotFound_ThrowsSessionNotFoundException")
        void getRecommendations_SessionNotFound_ThrowsSessionNotFoundException() {
            when(getRecommendationsUseCase.getRecommendations(SESSION_ID, USER_ID.toString()))
                    .thenThrow(new SessionNotFoundException(SESSION_ID));

            assertThrows(SessionNotFoundException.class,
                    () -> controller.getRecommendations(SESSION_ID, USER_ID));

            verify(getRecommendationsUseCase).getRecommendations(SESSION_ID, USER_ID.toString());
        }

        @Test
        @DisplayName("getRecommendations_UserNotOwner_ThrowsAccessDeniedException")
        void getRecommendations_UserNotOwner_ThrowsAccessDeniedException() {
            when(getRecommendationsUseCase.getRecommendations(SESSION_ID, USER_ID.toString()))
                    .thenThrow(new AccessDeniedException());

            assertThrows(AccessDeniedException.class,
                    () -> controller.getRecommendations(SESSION_ID, USER_ID));

            verify(getRecommendationsUseCase).getRecommendations(SESSION_ID, USER_ID.toString());
        }

        @Test
        @DisplayName("getRecommendations_SnapshotCorrupt_ThrowsSnapshotParseException")
        void getRecommendations_SnapshotCorrupt_ThrowsSnapshotParseException() {
            when(getRecommendationsUseCase.getRecommendations(SESSION_ID, USER_ID.toString()))
                    .thenThrow(new SnapshotParseException("Malformed JSON in workout snapshot"));

            assertThrows(SnapshotParseException.class,
                    () -> controller.getRecommendations(SESSION_ID, USER_ID));

            verify(getRecommendationsUseCase).getRecommendations(SESSION_ID, USER_ID.toString());
        }
    }
}
