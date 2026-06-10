# Implementation Plan: Theater Mode Recommendations

## Overview

This plan implements the recommendation engine that extracts prescribed exercise parameters (weight, reps, sets) from the workout snapshot and surfaces them through REST, WebSocket, and the Theater Mode UI. The implementation follows the hexagonal architecture: domain value object and engine first, then application service and ports, then inbound adapters (REST controller, WebSocket enrichment), and finally the frontend components.

## Tasks

- [x] 1. Create domain value object and exception
  - [x] 1.1 Create `ExerciseRecommendation` record in the session domain layer
    - Create `ExerciseRecommendation.java` as a Java record in `workout-session-service/src/main/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/session/domain/`
    - Fields: `sectionIndex` (int), `exerciseIndex` (int), `prescribedWeight` (String, nullable), `prescribedReps` (String, nullable), `prescribedSets` (Integer, nullable)
    - Compact constructor with validation: blank strings throw `IllegalArgumentException`, length > 50 throws, sets outside [1,100] throws
    - Add `isEmpty()` convenience method returning true when all three prescription fields are null
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [x] 1.2 Write property tests for `ExerciseRecommendation` value object construction
    - **Property 4: Value object rejects invalid construction**
    - **Validates: Requirements 7.4**
    - Create `ExerciseRecommendationPropertyTest.java` in `workout-session-service/src/test/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/property/`
    - Use jqwik `@Property(tries = 100)` with `@Provide` methods generating blank strings, empty strings, zero/negative sets, sets > 100
    - Verify all invalid inputs throw `IllegalArgumentException`

  - [x] 1.3 Write property test for serialization round-trip
    - **Property 5: Serialization round-trip**
    - **Validates: Requirements 7.6**
    - In `ExerciseRecommendationPropertyTest.java`, add property that generates valid `ExerciseRecommendation` instances, serializes to JSON via Jackson `ObjectMapper`, deserializes back, and asserts field equality
    - Use `@Property(tries = 100)` with a `@Provide` method for valid recommendations (non-blank strings ≤ 50 chars, sets in [1,100] or null)

  - [x] 1.4 Create `SnapshotParseException` in the session domain layer
    - Create `SnapshotParseException.java` in `workout-session-service/src/main/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/session/domain/`
    - Unchecked exception extending `RuntimeException` with a message constructor and a message + cause constructor
    - _Requirements: 4.5_

- [x] 2. Implement `RecommendationEngine` domain service
  - [x] 2.1 Create `RecommendationEngine` class in the session domain layer
    - Create `RecommendationEngine.java` in `workout-session-service/src/main/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/session/domain/`
    - Pure function class with no Spring/framework imports
    - Implement `computeAll(String workoutSnapshot, int weekNumber, int dayNumber)` → `List<ExerciseRecommendation>`
    - Implement `computeForSection(String workoutSnapshot, int weekNumber, int dayNumber, int sectionIndex)` → `List<ExerciseRecommendation>`
    - Parse JSON using Jackson `ObjectMapper` (instantiated internally, no DI needed for a pure domain class)
    - Branch on `modality` field: HYPERTROPHY populates all three fields (sets=0 → null), CROSSFIT populates only weight
    - Out-of-bounds section/exercise → all-null recommendation; malformed JSON → `SnapshotParseException`
    - Truncate weight/reps strings > 50 chars; clamp sets > 100 to 100
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3_

  - [x] 2.2 Write property test for HYPERTROPHY extraction
    - **Property 1: HYPERTROPHY extraction preserves snapshot values**
    - **Validates: Requirements 1.1, 1.3, 2.1, 2.2, 2.3, 2.4**
    - Create `RecommendationEnginePropertyTest.java` in `workout-session-service/src/test/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/property/`
    - Use `@Property(tries = 100)` with a `@Provide` that generates valid HYPERTROPHY snapshots with random exercise counts, weights, reps, sets
    - Assert each output recommendation matches the snapshot's exercise fields (weight/reps preserved, sets=0 → null)

  - [x] 2.3 Write property test for CROSSFIT extraction
    - **Property 2: CROSSFIT extraction returns only weight**
    - **Validates: Requirements 3.1, 3.2, 3.3**
    - In `RecommendationEnginePropertyTest.java`, add property generating valid CROSSFIT snapshots
    - Assert `prescribedReps` and `prescribedSets` are always null regardless of snapshot values; `prescribedWeight` matches snapshot

  - [x] 2.4 Write property test for output domain invariants
    - **Property 3: Output domain invariants**
    - **Validates: Requirements 7.1, 7.2, 7.3**
    - In `RecommendationEnginePropertyTest.java`, add property generating mixed modality snapshots (HYPERTROPHY and CROSSFIT)
    - Assert every produced recommendation satisfies: sets null or [1,100], reps null or non-blank ≤ 50 chars, weight null or non-blank ≤ 50 chars

  - [x] 2.5 Write unit tests for `RecommendationEngine` edge cases
    - Create `RecommendationEngineTest.java` in `workout-session-service/src/test/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/unit/`
    - Test naming: `MethodName_StateUnderTest_ExpectedBehaviour`
    - Cases: malformed JSON → `SnapshotParseException`; out-of-bounds section index → all-null; out-of-bounds exercise index → all-null; empty sections array → empty list; null weight in exercise → null in recommendation
    - _Requirements: 1.2, 1.4_

- [x] 3. Checkpoint - Ensure all domain tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement application layer and inbound port
  - [x] 4.1 Create `GetRecommendationsUseCase` inbound port interface
    - Create `GetRecommendationsUseCase.java` in `workout-session-service/src/main/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/session/ports/inbound/`
    - Methods: `getRecommendations(UUID sessionId, String userId)` → `List<ExerciseRecommendation>`; `getRecommendationsForSection(UUID sessionId, String userId, int sectionIndex)` → `List<ExerciseRecommendation>`
    - _Requirements: 4.1, 5.1_

  - [x] 4.2 Create `RecommendationService` implementing the use case
    - Create `RecommendationService.java` in `workout-session-service/src/main/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/session/application/`
    - Inject `SessionRepository` (outbound port) and instantiate `RecommendationEngine`
    - `getRecommendations`: load session, verify user ownership (throw `AccessDeniedException` if mismatch), delegate to `engine.computeAll(session.getWorkoutSnapshot(), session.getWeekNumber(), session.getDayNumber())`
    - `getRecommendationsForSection`: same ownership check, delegate to `engine.computeForSection(...)`
    - Throw `SessionNotFoundException` if session not found (reuse existing exception)
    - _Requirements: 4.2, 4.3, 4.4_

  - [x] 4.3 Write unit tests for `RecommendationService`
    - Create `RecommendationServiceTest.java` in `workout-session-service/src/test/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/unit/`
    - Mock `SessionRepository`; verify happy path delegates to engine; session not found throws; wrong user throws `AccessDeniedException`
    - _Requirements: 4.2, 4.3, 4.4_

- [x] 5. Implement REST adapter
  - [x] 5.1 Create `RecommendationController` and response DTOs
    - Create `RecommendationsResponse.java` (record with nested `SectionRecommendations` and `ExerciseRecommendationDto`) in `workout-session-service/src/main/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/session/adapters/inbound/dto/`
    - Create `RecommendationController.java` in `workout-session-service/src/main/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/session/adapters/inbound/`
    - `@GetMapping("/{sessionId}/recommendations")` on base path `/api/v1/sessions`
    - Map domain `ExerciseRecommendation` list → grouped `RecommendationsResponse` (group by sectionIndex)
    - Handle exceptions: `SessionNotFoundException` → 404, `AccessDeniedException` → 403, `SnapshotParseException` → 500 with "Workout snapshot is corrupted"
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [x] 5.2 Write unit tests for `RecommendationController`
    - Create `RecommendationControllerTest.java` in `workout-session-service/src/test/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/unit/`
    - Use MockMvc or direct invocation with mocked `GetRecommendationsUseCase`
    - Verify: 200 response shape matches design DTO; 404 when session not found; 403 when not owner; 500 when snapshot corrupt; empty array for no exercises
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [x] 6. Implement WebSocket enrichment
  - [x] 6.1 Modify `StompSessionNotifier` to include recommendations in session state messages
    - Edit `StompSessionNotifier.java` in `workout-session-service/src/main/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/session/adapters/inbound/`
    - Add `RecommendationEngine` dependency (or `GetRecommendationsUseCase`)
    - When publishing session state, call `engine.computeForSection(...)` with the session's `currentSectionIndex`
    - Add `recommendations` field (list of `ExerciseRecommendationDto`) to the session state message DTO
    - If `computeForSection` throws, set `recommendations` to empty list and still publish the message
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [x] 6.2 Write property test for WebSocket message scoping
    - **Property 6: WebSocket message scoped to current section**
    - **Validates: Requirements 5.1, 5.3**
    - In `RecommendationEnginePropertyTest.java`, add property generating snapshots with multiple sections and arbitrary `currentSectionIndex`
    - Assert that `computeForSection(snapshot, week, day, currentSectionIndex)` returns only recommendations whose `sectionIndex` equals `currentSectionIndex`
    - Use `@Property(tries = 100)`

  - [x] 6.3 Write unit test for `StompSessionNotifier` recommendation enrichment
    - In `workout-session-service/src/test/java/com/gmail/ramawthar/priyash/hybridstrength/workoutsession/unit/`, create or extend a test for `StompSessionNotifier`
    - Verify recommendations field populated on happy path; verify empty list on engine failure; verify message still published on engine failure
    - _Requirements: 5.1, 5.4_

- [x] 7. Checkpoint - Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement frontend recommendation hook and types
  - [x] 8.1 Create TypeScript types for recommendations
    - Create `recommendation.ts` in `workout-coach-ui/src/types/`
    - Define `ExerciseRecommendationDto` interface: `{ exerciseIndex: number; prescribedWeight: string | null; prescribedReps: string | null; prescribedSets: number | null }`
    - Define `SectionRecommendations` interface: `{ sectionIndex: number; exercises: ExerciseRecommendationDto[] }`
    - Define `RecommendationsResponse` interface: `{ sections: SectionRecommendations[] }`
    - _Requirements: 6.1, 6.2_

  - [x] 8.2 Create `useRecommendations` hook
    - Create `useRecommendations.ts` in `workout-coach-ui/src/hooks/`
    - Fetch recommendations via REST `GET /api/v1/sessions/{sessionId}/recommendations` on mount and section change
    - Merge with WebSocket `recommendations` field from STOMP session state messages
    - Expose: `recommendations` (current section's `ExerciseRecommendationDto[]`), `isLoading`, `error`
    - On fetch failure: set recommendations to empty array, do not block UI
    - _Requirements: 6.6, 6.7_

  - [x] 8.3 Write unit tests for `useRecommendations` hook
    - Create `useRecommendations.test.ts` in `workout-coach-ui/src/hooks/__tests__/`
    - Test: REST fetch on mount returns data; WebSocket update merges; error sets empty array; loading state transitions
    - _Requirements: 6.6, 6.7_

- [x] 9. Implement frontend `RecommendationBadge` component
  - [x] 9.1 Create `RecommendationBadge` component
    - Create `RecommendationBadge.tsx` in `workout-coach-ui/src/features/theater/`
    - Props: `prescribedWeight`, `prescribedReps`, `prescribedSets`, `isLoading`
    - HYPERTROPHY format: display non-null fields joined by " · " in order [weight, reps, sets]
    - CROSSFIT format: display only weight
    - If all fields null: render nothing (return null)
    - If loading: render a loading indicator (skeleton/spinner)
    - Render as read-only text, positioned to not overlap set-logging controls
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

  - [x] 9.2 Write property test for badge format correctness
    - **Property 7: Recommendation badge format correctness**
    - **Validates: Requirements 6.1, 6.2, 6.4**
    - Create `RecommendationBadge.property.test.ts` in `workout-coach-ui/src/features/theater/__tests__/`
    - Use `fast-check` with `fc.property` and minimum 100 iterations (`numRuns: 100`)
    - Generate arbitrary non-null field combinations; assert output string contains exactly the non-null values joined by " · "

  - [x] 9.3 Write unit tests for `RecommendationBadge` component
    - Create `RecommendationBadge.test.tsx` in `workout-coach-ui/src/features/theater/__tests__/`
    - Test: HYPERTROPHY full format "80kg · 8-10 · 4"; CROSSFIT weight-only "60kg"; all-null renders nothing; partial nulls omit missing fields; loading state shows indicator
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.6_

- [x] 10. Integrate `RecommendationBadge` into `ExerciseCard` and Theater Mode page
  - [x] 10.1 Modify existing exercise rendering to include `RecommendationBadge`
    - Edit the relevant exercise card component in `workout-coach-ui/src/features/theater/` (likely `ExerciseChecklist.tsx` or create an `ExerciseCard.tsx` wrapper)
    - Pass recommendation data from `useRecommendations` hook to `RecommendationBadge` for each exercise in the current section
    - Position badge above or beside set-logging controls; ensure it does not overlap or disable inputs
    - On error: hide badge area, do not block set logging
    - _Requirements: 6.1, 6.2, 6.5, 6.7_

  - [x] 10.2 Wire `useRecommendations` into `TheaterModePage`
    - Edit `TheaterModePage.tsx` in `workout-coach-ui/src/features/theater/`
    - Call `useRecommendations(sessionId, currentSectionIndex)` and pass data down to exercise card components
    - _Requirements: 6.6, 6.7_

  - [x] 10.3 Write unit tests for `ExerciseCard` integration with badge
    - In `workout-coach-ui/src/features/theater/__tests__/`, create or extend exercise card tests
    - Verify badge does not overlap input controls; badge is read-only; badge hidden on error
    - _Requirements: 6.5, 6.7_

- [x] 11. Add test resource fixtures
  - [x] 11.1 Create JSON snapshot fixture files for tests
    - Create `workout-session-service/src/test/resources/snapshots/hypertrophy-snapshot.json` with a valid HYPERTROPHY workout snapshot (multiple sections, multiple exercises with varied weight/reps/sets values including nulls and zeros)
    - Create `workout-session-service/src/test/resources/snapshots/crossfit-snapshot.json` with a valid CROSSFIT workout snapshot
    - Create `workout-session-service/src/test/resources/snapshots/malformed-snapshot.json` with invalid JSON for error path testing
    - _Requirements: 1.1, 2.1, 3.1_

- [x] 12. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document (Properties 1–7)
- Unit tests validate specific examples and edge cases
- The `RecommendationEngine` is a pure function class with no Spring dependencies — ideal for property-based testing
- Backend tests go in `src/test/java/.../unit/`, `.../property/`, `.../integration/` per steering docs
- Frontend property tests use `fast-check` with minimum 100 iterations (`numRuns: 100`)
- Backend property tests use jqwik with `@Property(tries = 100)`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.4"] },
    { "id": 1, "tasks": ["1.2", "1.3", "2.1", "11.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "2.5", "4.1"] },
    { "id": 3, "tasks": ["4.2", "8.1"] },
    { "id": 4, "tasks": ["4.3", "5.1", "6.1", "8.2"] },
    { "id": 5, "tasks": ["5.2", "6.2", "6.3", "8.3", "9.1"] },
    { "id": 6, "tasks": ["9.2", "9.3", "10.1"] },
    { "id": 7, "tasks": ["10.2", "10.3"] }
  ]
}
```
