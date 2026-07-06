package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit.vault;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.dto.ErrorResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.VaultController;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto.CreateManualProgramRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto.DayAssignmentRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound.*;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VaultController's validateDayAssignments logic.
 * Tests the controller's custom validation for day assignment requests
 * exercised through the public createManualProgram method.
 * No Spring context — plain Java instantiation with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VaultControllerValidationTest {

    @Mock
    private ListProgramsUseCase listProgramsUseCase;
    @Mock
    private GetProgramUseCase getProgramUseCase;
    @Mock
    private UpdateProgramUseCase updateProgramUseCase;
    @Mock
    private DeleteProgramUseCase deleteProgramUseCase;
    @Mock
    private CopyProgramUseCase copyProgramUseCase;
    @Mock
    private SearchProgramsUseCase searchProgramsUseCase;
    @Mock
    private CreateManualProgramUseCase createManualProgramUseCase;
    @Mock
    private GetProgramDaysUseCase getProgramDaysUseCase;
    @Mock
    private HttpServletRequest httpServletRequest;

    private VaultController controller;

    @BeforeEach
    void setUp() {
        controller = new VaultController(
                listProgramsUseCase,
                getProgramUseCase,
                updateProgramUseCase,
                deleteProgramUseCase,
                copyProgramUseCase,
                searchProgramsUseCase,
                createManualProgramUseCase,
                getProgramDaysUseCase
        );

        when(httpServletRequest.getRequestURI()).thenReturn("/api/v1/vault/programs");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("validateDayAssignments")
    class ValidateDayAssignments {

        @Test
        @DisplayName("ValidateDayAssignments_CopiedDayMissingSourceProgramId_Returns400")
        void ValidateDayAssignments_CopiedDayMissingSourceProgramId_Returns400() {
            DayAssignmentRequest day = new DayAssignmentRequest(
                    1, "copied_day", null, null,
                    null, // sourceProgramId is null
                    1, 1
            );
            CreateManualProgramRequest request = new CreateManualProgramRequest("My Program", List.of(day));

            ResponseEntity<?> response = controller.createManualProgram(request, httpServletRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertInstanceOf(ErrorResponse.class, response.getBody());
            ErrorResponse error = (ErrorResponse) response.getBody();
            assertTrue(error.message().contains("sourceProgramId is required"));
            verify(createManualProgramUseCase, never()).createManualProgram(any());
        }

        @Test
        @DisplayName("ValidateDayAssignments_CopiedDayBlankSourceProgramId_Returns400")
        void ValidateDayAssignments_CopiedDayBlankSourceProgramId_Returns400() {
            DayAssignmentRequest day = new DayAssignmentRequest(
                    1, "copied_day", null, null,
                    "   ", // sourceProgramId is blank
                    1, 1
            );
            CreateManualProgramRequest request = new CreateManualProgramRequest("My Program", List.of(day));

            ResponseEntity<?> response = controller.createManualProgram(request, httpServletRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertInstanceOf(ErrorResponse.class, response.getBody());
            ErrorResponse error = (ErrorResponse) response.getBody();
            assertTrue(error.message().contains("sourceProgramId is required"));
            verify(createManualProgramUseCase, never()).createManualProgram(any());
        }

        @Test
        @DisplayName("ValidateDayAssignments_CopiedDayNonPositiveWeekNumber_Returns400")
        void ValidateDayAssignments_CopiedDayNonPositiveWeekNumber_Returns400() {
            String validUuid = UUID.randomUUID().toString();
            DayAssignmentRequest day = new DayAssignmentRequest(
                    1, "copied_day", null, null,
                    validUuid,
                    0, // sourceWeekNumber is 0 (non-positive)
                    1
            );
            CreateManualProgramRequest request = new CreateManualProgramRequest("My Program", List.of(day));

            ResponseEntity<?> response = controller.createManualProgram(request, httpServletRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertInstanceOf(ErrorResponse.class, response.getBody());
            ErrorResponse error = (ErrorResponse) response.getBody();
            assertTrue(error.message().contains("sourceWeekNumber must be a positive integer"));
            verify(createManualProgramUseCase, never()).createManualProgram(any());
        }

        @Test
        @DisplayName("ValidateDayAssignments_CopiedDayNegativeWeekNumber_Returns400")
        void ValidateDayAssignments_CopiedDayNegativeWeekNumber_Returns400() {
            String validUuid = UUID.randomUUID().toString();
            DayAssignmentRequest day = new DayAssignmentRequest(
                    1, "copied_day", null, null,
                    validUuid,
                    -5, // sourceWeekNumber is negative
                    1
            );
            CreateManualProgramRequest request = new CreateManualProgramRequest("My Program", List.of(day));

            ResponseEntity<?> response = controller.createManualProgram(request, httpServletRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertInstanceOf(ErrorResponse.class, response.getBody());
            ErrorResponse error = (ErrorResponse) response.getBody();
            assertTrue(error.message().contains("sourceWeekNumber must be a positive integer"));
            verify(createManualProgramUseCase, never()).createManualProgram(any());
        }

        @Test
        @DisplayName("ValidateDayAssignments_CopiedDayNullWeekNumber_Returns400")
        void ValidateDayAssignments_CopiedDayNullWeekNumber_Returns400() {
            String validUuid = UUID.randomUUID().toString();
            DayAssignmentRequest day = new DayAssignmentRequest(
                    1, "copied_day", null, null,
                    validUuid,
                    null, // sourceWeekNumber is null
                    1
            );
            CreateManualProgramRequest request = new CreateManualProgramRequest("My Program", List.of(day));

            ResponseEntity<?> response = controller.createManualProgram(request, httpServletRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertInstanceOf(ErrorResponse.class, response.getBody());
            ErrorResponse error = (ErrorResponse) response.getBody();
            assertTrue(error.message().contains("sourceWeekNumber must be a positive integer"));
            verify(createManualProgramUseCase, never()).createManualProgram(any());
        }

        @Test
        @DisplayName("ValidateDayAssignments_UnknownType_Returns400WithValidTypesList")
        void ValidateDayAssignments_UnknownType_Returns400WithValidTypesList() {
            DayAssignmentRequest day = new DayAssignmentRequest(
                    1, "unknown_type", null, null,
                    null, null, null
            );
            CreateManualProgramRequest request = new CreateManualProgramRequest("My Program", List.of(day));

            ResponseEntity<?> response = controller.createManualProgram(request, httpServletRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertInstanceOf(ErrorResponse.class, response.getBody());
            ErrorResponse error = (ErrorResponse) response.getBody();
            assertTrue(error.message().contains("Invalid day type"));
            assertTrue(error.message().contains("unknown_type"));
            assertTrue(error.message().contains("activity"));
            assertTrue(error.message().contains("copied_day"));
            verify(createManualProgramUseCase, never()).createManualProgram(any());
        }

        @Test
        @DisplayName("ValidateDayAssignments_ValidCopiedDay_PassesValidation")
        void ValidateDayAssignments_ValidCopiedDay_PassesValidation() {
            String validUuid = UUID.randomUUID().toString();
            DayAssignmentRequest day = new DayAssignmentRequest(
                    1, "copied_day", null, null,
                    validUuid, 2, 3
            );
            CreateManualProgramRequest request = new CreateManualProgramRequest("My Program", List.of(day));

            // Set up security context so resolveOwnerUserId works after validation passes
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("user-123", null, List.of())
            );

            UUID programId = UUID.randomUUID();
            when(createManualProgramUseCase.createManualProgram(any())).thenReturn(programId);

            ResponseEntity<?> response = controller.createManualProgram(request, httpServletRequest);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            verify(createManualProgramUseCase).createManualProgram(any());
        }
    }
}
