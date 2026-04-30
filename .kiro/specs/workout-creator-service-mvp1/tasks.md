# Implementation Plan: Workout Creator Service MVP1

## Overview

Implement the AI-powered Workout and Program generation service following hexagonal architecture. The service accepts natural language descriptions, calls Google Gemini, parses responses into domain objects, and returns both raw and structured results. MVP1 covers generation, parsing, formatting, content sanitisation, and Flyway schema setup — no persistence or CRUD.

## Tasks

- [x] 1. Bootstrap project structure and configuration
  - [x] 1.1 Create the `workout-creator-service` Maven module with Spring Boot 3.x parent, Java 17, and required dependencies
    - Add spring-boot-starter-web, spring-boot-starter-security, spring-boot-starter-validation, spring-boot-starter-data-jpa, flyway-core, flyway-database-postgresql, postgresql driver, resilience4j-spring-boot3, jackson-databind, jqwik, junit-jupiter, mockito, testcontainers-postgresql, wiremock, logstash-logback-encoder
    - Create `WorkoutCreatorApplication.java` main class
    - Create `application.yml`, `application-dev.yml`, `applicati--
    on-prod.yml`, `application-test.yml`, and `logback-spring.xml`
    - _Requirements: 7.1, 7.2_

  - [x] 1.2 Implement `SecurityConfig` with JWT RS256 authentication filter
    - Create `SecurityConfig.java` with `SecurityFilterChain` bean — stateless sessions, CSRF disabled, `/api/v1/workouts/generate` requires authentication
    - Create `JwtAuthenticationFilter.java` that validates RS256 JWT from Authorization header and sets SecurityContext
    - _Requirements: 1.3_

  - [x] 1.3 Implement `GeminiConfig` with Resilience4j circuit breaker and RestClient bean
    - Create `GeminiConfig.java` configuring a `RestClient` bean for Gemini API calls
    - Configure Resilience4j circuit breaker (`gemini`) and time limiter (`gemini`) in `application.yml` per design spec (sliding-window-size: 10, failure-rate-threshold: 50, wait-duration: 30s, timeout: 10s)
    - _Requirements: 6.3, 6.4_

- [x] 2. Implement domain model and enumerations
  - [x] 2.1 Create domain enumerations and value objects
    - Create `GenerationScope` enum (DAY, WEEK, FOUR_WEEK) in `generation/domain/`
    - Create `TrainingStyle` enum (CROSSFIT, HYPERTROPHY, STRENGTH) in `generation/domain/`
    - Create `SectionType` enum (STRENGTH, AMRAP, EMOM, TABATA, FOR_TIME, ACCESSORY) in `generation/domain/`
    - _Requirements: 1.1, 4.1_

  - [x] 2.2 Create core domain objects — Exercise, Section, Workout, Program
    - Create `Exercise.java` with name, sets, reps, weight, restSeconds fields — pure Java, no framework imports
    - Create `Section.java` with name, type, exercises list, and timing fields (timeCapMinutes, intervalSeconds, totalRounds, workIntervalSeconds, restIntervalSeconds) — enforce timing invariants based on SectionType
    - Create `Workout.java` with name, description, trainingStyle, sections list
    - Create `Program.java` with name, description, scope, trainingStyles list, workouts list
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8_

  - [x] 2.3 Write property test for Section timing invariants
    - **Property 8: Section timing fields match SectionType**
    - **Validates: Requirements 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8**

  - [x] 2.4 Create GenerationCommand and GenerationResult value objects
    - Create `GenerationCommand.java` with userId, description, scope, trainingStyles
    - Create `GenerationResult.java` with rawGeminiResponse, workout, program, parsingError
    - _Requirements: 5.1, 5.2, 5.4_

- [x] 3. Implement ContentSanitiser and PromptBuilder
  - [x] 3.1 Implement `ContentSanitiser` in the domain layer
    - Strip HTML tags, script injections, and control characters from Gemini output
    - Preserve all plain-text content that is not part of a tag
    - Pure string manipulation — no framework imports
    - _Requirements: 2.3_

  - [x] 3.2 Write property test for ContentSanitiser
    - **Property 6: Sanitiser removes unsafe content**
    - **Validates: Requirements 2.3**

  - [x] 3.3 Implement `PromptBuilder` in the domain layer
    - Build structured prompt string from description, scope, and trainingStyles
    - Include all training style names in the prompt
    - Include schema constraints for expected Gemini response format
    - _Requirements: 1.6_

  - [ ]* 3.4 Write property test for PromptBuilder
    - **Property 4: Prompt contains all requested training styles**
    - **Validates: Requirements 1.6**

- [ ] 4. Implement WorkoutParser and WorkoutFormatter
  - [ ] 4.1 Implement `WorkoutParser` in the domain layer
    - Parse sanitised Gemini text into Workout (for DAY scope) or Program (for WEEK/FOUR_WEEK scope)
    - Throw checked `ParsingException` with human-readable message on failure
    - Stateless utility — no framework imports
    - _Requirements: 2.1, 2.2_

  - [ ] 4.2 Implement `WorkoutFormatter` in the domain layer
    - Format Workout or Program domain object into human-readable text representation
    - Include all Section names, SectionTypes, Exercise names, sets, reps, weight, restSeconds in output
    - Stateless utility — no framework imports
    - _Requirements: 3.1, 3.3_

  - [ ]* 4.3 Write property test for parse-format round trip
    - **Property 5: Parse–format round trip**
    - **Validates: Requirements 2.1, 3.1, 3.2**

  - [ ]* 4.4 Write property test for formatted output completeness
    - **Property 7: Formatted output contains all domain fields**
    - **Validates: Requirements 3.3**

  - [ ]* 4.5 Write unit tests for WorkoutParser
    - Test specific parsing examples for each SectionType (STRENGTH, AMRAP, EMOM, TABATA, FOR_TIME, ACCESSORY)
    - Test malformed input, empty input, missing sections
    - _Requirements: 2.1, 2.2_

  - [ ]* 4.6 Write unit tests for WorkoutFormatter
    - Test formatting examples for each SectionType
    - Test edge cases: empty sections, missing optional fields (null weight, null restSeconds)
    - _Requirements: 3.1, 3.3_

- [ ] 5. Checkpoint — Ensure all domain tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Implement ports and application service
  - [ ] 6.1 Create inbound port `GenerateWorkoutUseCase`
    - Define interface with `GenerationResult generate(GenerationCommand command)` method in `generation/ports/inbound/`
    - _Requirements: 1.1_

  - [ ] 6.2 Create outbound port `GeminiClient`
    - Define interface with `String generate(String prompt)` method in `generation/ports/outbound/`
    - Document that it throws `GeminiUnavailableException` on failure
    - _Requirements: 6.1_

  - [ ] 6.3 Implement `GenerationService` application service
    - Implement `GenerateWorkoutUseCase` — orchestrate PromptBuilder, GeminiClient, ContentSanitiser, WorkoutParser
    - On successful parse: return GenerationResult with raw text + parsed domain object + null parsingError
    - On parse failure: catch ParsingException, return GenerationResult with raw text + null domain objects + error message
    - _Requirements: 1.1, 2.1, 2.2, 5.1, 5.2, 5.3, 5.4_

  - [ ]* 6.4 Write unit tests for GenerationService
    - Mock GeminiClient — test success path, parse failure path, Gemini error path
    - Verify scope DAY returns workout, scope WEEK/FOUR_WEEK returns program
    - Verify parse failure returns 200-style result with raw text and error message
    - _Requirements: 1.1, 5.1, 5.2, 5.3, 5.4_

  - [ ]* 6.5 Write property tests for GenerationService
    - **Property 1: Scope determines result type**
    - **Property 9: Successful generation result structure**
    - **Property 10: Failed parse result structure**
    - **Validates: Requirements 1.1, 1.5, 5.1, 5.2, 5.3, 5.4**

- [ ] 7. Implement inbound adapter (REST controller) and DTOs
  - [ ] 7.1 Create request/response DTOs
    - Create `GenerateRequest` record with Jakarta Bean Validation: `@NotBlank description`, `@NotNull scope`, `@NotEmpty trainingStyles`
    - Add custom validation: DAY scope requires exactly one training style
    - Create `GenerateResponse` record with rawGeminiResponse, WorkoutDto, ProgramDto, parsingError
    - Create `WorkoutDto`, `ProgramDto`, `SectionDto`, `ExerciseDto` for API response mapping
    - _Requirements: 1.2, 1.4_

  - [ ] 7.2 Implement `GenerationController`
    - `POST /api/v1/workouts/generate` — validate request, extract userId from JWT, map to GenerationCommand, call use case, map result to GenerateResponse, return 200 OK
    - _Requirements: 1.1, 1.3_

  - [ ]* 7.3 Write property tests for request validation
    - **Property 2: Invalid requests are rejected**
    - **Property 3: DAY scope requires exactly one training style**
    - **Validates: Requirements 1.2, 1.4**

- [ ] 8. Implement outbound adapter (Gemini REST client) and error handling
  - [ ] 8.1 Implement `GeminiRestClient` outbound adapter
    - Implement `GeminiClient` port using Spring `RestClient`
    - Annotate with `@CircuitBreaker(name = "gemini")` and `@TimeLimiter(name = "gemini")`
    - Fallback method throws `GeminiUnavailableException`
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [ ] 8.2 Implement `GlobalExceptionHandler` and exception classes
    - Create `GeminiUnavailableException` — mapped to 502 Bad Gateway
    - Create `GlobalExceptionHandler` with `@RestControllerAdvice` — handle validation errors (400), GeminiUnavailableException (502), authentication errors (401), and catch-all (500)
    - Use standard `ErrorResponse` and `ValidationErrorResponse` shapes from api-standards
    - _Requirements: 1.2, 6.1, 6.2_

- [ ] 9. Implement Flyway database migrations
  - [ ] 9.1 Create Flyway migration scripts V100–V104
    - `V100__create_workouts_table.sql` — workouts table with user_id index
    - `V101__create_sections_table.sql` — sections table with workout_id FK and index
    - `V102__create_exercises_table.sql` — exercises table with section_id FK and index
    - `V103__create_programs_table.sql` — programs table with user_id index
    - `V104__create_program_workouts_table.sql` — program_workouts join table with unique constraint on (program_id, day_number)
    - _Requirements: 7.2, 7.3, 7.5_

- [ ] 10. Checkpoint — Ensure all unit and property tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Integration tests
  - [ ]* 11.1 Write Flyway migration integration test
    - Verify all V100–V104 migrations apply cleanly against Testcontainers PostgreSQL
    - Verify tables exist with correct columns, foreign keys, and indexes
    - _Requirements: 7.2, 7.3, 7.5_

  - [ ]* 11.2 Write generation endpoint integration test
    - Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `WebTestClient` and Testcontainers PostgreSQL
    - Stub Gemini API with WireMock
    - Test valid request → 200 with parsed result
    - Test invalid request → 400 with validation errors
    - Test Gemini failure → 502 Bad Gateway
    - Test missing JWT → 401 Unauthorised
    - Use test JWT signed with test RSA key pair
    - _Requirements: 1.1, 1.2, 1.3, 6.1, 6.2_

- [ ] 12. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document (Properties 1–10)
- Unit tests validate specific examples and edge cases
- All domain objects are pure Java — no Spring/JPA imports
- Integration tests use Testcontainers PostgreSQL and WireMock for Gemini stubbing
