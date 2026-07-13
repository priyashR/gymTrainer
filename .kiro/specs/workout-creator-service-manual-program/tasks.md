# Implementation Plan: Manual Program Creation Backend Endpoint

## Overview

Implement a `POST /api/v1/vault/programs` endpoint in the existing `VaultController` that allows authenticated users to manually create a training program by assigning existing vault workouts or external activities to numbered training days. The implementation follows hexagonal architecture: domain models first, then ports, then application service logic, then adapters (controller + repository), and finally tests.

## Tasks

- [x] 1. Create domain models for manual program creation
  - [x] 1.1 Create `DayAssignmentType` enum and `DayAssignment` record in `vault/domain/`
    - Create `DayAssignmentType` enum with `WORKOUT` and `ACTIVITY` values
    - Create `DayAssignment` record with fields: `dayNumber` (int), `type` (DayAssignmentType), `workoutId` (UUID, nullable), `activityType` (String, nullable)
    - No framework imports — pure domain objects
    - _Requirements: 5.2_

  - [x] 1.2 Create `ManualProgram` record in `vault/domain/`
    - Create `ManualProgram` record with fields: `id` (UUID), `name` (String), `ownerUserId` (String), `contentSource` (ContentSource), `dayAssignments` (List<DayAssignment>), `createdAt` (Instant), `updatedAt` (Instant)
    - Import the existing `ContentSource` enum from `common/model/`
    - _Requirements: 5.1_

- [x] 2. Create inbound port and command
  - [x] 2.1 Create `CreateManualProgramCommand` record in `vault/ports/inbound/`
    - Fields: `programName` (String), `ownerUserId` (String), `dayAssignments` (List<DayAssignment>)
    - _Requirements: 1.1_

  - [x] 2.2 Create `CreateManualProgramUseCase` interface in `vault/ports/inbound/`
    - Single method: `UUID createManualProgram(CreateManualProgramCommand command)`
    - _Requirements: 1.1, 1.2_

- [x] 3. Extend outbound port and create exception
  - [x] 3.1 Add `saveManualProgram(ManualProgram)` method to existing `VaultProgramRepository` interface
    - Returns `void` — the caller already has the generated UUID
    - _Requirements: 5.1, 5.2_

  - [x] 3.2 Create `WorkoutNotFoundException` in `common/exception/`
    - Extends `RuntimeException`
    - Constructor accepts `UUID workoutId`
    - Message format: `"Workout not found in vault: " + workoutId`
    - Include `getWorkoutId()` accessor
    - _Requirements: 4.1, 4.2, 6.1_

- [x] 4. Implement application service logic
  - [x] 4.1 Add `CreateManualProgramUseCase` implementation to existing `VaultService`
    - Add `CreateManualProgramUseCase` to the implements clause
    - Inject nothing new — reuse existing `VaultProgramRepository`
    - Implementation: verify all workout-type day assignments exist via `existsByIdAndOwner`, construct `ManualProgram` with generated UUID and `ContentSource.MANUAL`, delegate to `saveManualProgram`
    - Throw `WorkoutNotFoundException` if any referenced workout is not in the user's vault
    - _Requirements: 1.1, 4.1, 4.2, 5.1_

  - [x] 4.2 Write unit tests for `VaultService.createManualProgram`
    - Test class: `CreateManualProgramServiceTest` in `src/test/java/.../workoutcreator/unit/vault/`
    - Tests: `CreateManualProgram_ValidCommand_ReturnsProgramId`, `CreateManualProgram_WorkoutNotInVault_ThrowsWorkoutNotFoundException`, `CreateManualProgram_WorkoutInOtherUsersVault_ThrowsWorkoutNotFoundException`, `CreateManualProgram_DatabaseError_PropagatesAsRuntimeException`
    - Mock `VaultProgramRepository` with Mockito
    - _Requirements: 4.1, 4.2, 5.1, 6.1_

- [x] 5. Checkpoint - Ensure domain, ports, and service compile
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Create Flyway migration and JPA adapter
  - [x] 6.1 Create Flyway migration `V103__create_day_assignments.sql`
    - Path: `src/main/resources/db/migration/V103__create_day_assignments.sql`
    - Create `day_assignments` table with columns: `id` (UUID PK), `program_id` (UUID NOT NULL, FK to programs ON DELETE CASCADE), `day_number` (INTEGER NOT NULL), `assignment_type` (VARCHAR(20) NOT NULL), `workout_id` (UUID nullable), `activity_type` (VARCHAR(100) nullable)
    - Add UNIQUE constraint on `(program_id, day_number)`
    - Add index `idx_day_assignments_program` on `program_id`
    - _Requirements: 5.1, 5.2_

  - [x] 6.2 Create `DayAssignmentJpaEntity` in `vault/adapters/outbound/`
    - JPA entity mapped to `day_assignments` table
    - Fields map to all columns in the migration
    - ManyToOne relationship to `ProgramJpaEntity`
    - _Requirements: 5.2_

  - [x] 6.3 Implement `saveManualProgram` in `JpaVaultProgramRepository`
    - Insert a row into `programs` with `duration_weeks=0`, `goal='Manual Program'`, `equipment_profile='[]'`, `content_source='MANUAL'`
    - Batch-insert rows into `day_assignments` for each day assignment
    - Use existing `ProgramSpringDataRepository` and a new `DayAssignmentSpringDataRepository`
    - _Requirements: 5.1, 5.2_

- [x] 7. Create request/response DTOs and controller endpoint
  - [x] 7.1 Create `CreateManualProgramRequest` and `DayAssignmentRequest` DTOs in `vault/adapters/inbound/dto/`
    - `CreateManualProgramRequest`: `@NotBlank programName`, `@Size(max=255) programName`, `@NotNull @Size(min=1) List<DayAssignmentRequest> days`
    - `DayAssignmentRequest`: `@Min(1) int dayNumber`, `@NotBlank String type`, `String workoutId`, `String activityType`
    - _Requirements: 2.1, 2.2, 3.1, 3.4_

  - [x] 7.2 Create `CreateProgramResponse` record in `vault/adapters/inbound/dto/`
    - Single field: `String id`
    - _Requirements: 1.2_

  - [x] 7.3 Add `createManualProgram` POST endpoint to `VaultController`
    - `@PostMapping` with `consumes` and `produces` JSON
    - Accept `@Valid @RequestBody CreateManualProgramRequest`
    - Resolve `ownerUserId` from JWT via `resolveOwnerUserId()`
    - Add custom validation: conditional required fields (workoutId for workout type, activityType for activity type), duplicate day numbers
    - Map request to `CreateManualProgramCommand` (convert type strings to `DayAssignmentType`, parse workoutId strings to UUIDs)
    - Return 201 Created with `CreateProgramResponse`
    - _Requirements: 1.1, 1.2, 1.3, 3.2, 3.3, 3.5_

  - [x] 7.4 Add `WorkoutNotFoundException` handler to `GlobalExceptionHandler`
    - Return 400 Bad Request with `ErrorResponse` containing the exception message
    - Also ensure `MethodArgumentNotValidException` handler exists for bean validation errors
    - _Requirements: 4.1, 6.1, 6.2_

- [x] 8. Checkpoint - Ensure build compiles and existing tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Write property-based tests
  - [x] 9.1 Write property test: whitespace-only program names are rejected
    - **Property 1: Whitespace-only program names are rejected**
    - **Validates: Requirements 2.1**
    - Test class: `ManualProgramPropertyTest` in `src/test/java/.../workoutcreator/property/vault/`
    - Generate arbitrary whitespace-only strings (including null and empty), verify controller/validation returns 400
    - `@Property(tries = 100)`

  - [x] 9.2 Write property test: conditional required field validation
    - **Property 2: Conditional required field validation**
    - **Validates: Requirements 3.2, 3.3**
    - Generate day assignments with type="workout" and null/blank workoutId, OR type="activity" and null/blank activityType, verify 400 rejection
    - `@Property(tries = 100)`

  - [x] 9.3 Write property test: non-positive day numbers are rejected
    - **Property 3: Non-positive day numbers are rejected**
    - **Validates: Requirements 3.4**
    - Generate day assignments with dayNumber <= 0 (zero and negative integers), verify 400 rejection
    - `@Property(tries = 100)`

  - [x] 9.4 Write property test: duplicate day numbers are rejected
    - **Property 4: Duplicate day numbers are rejected**
    - **Validates: Requirements 3.5**
    - Generate lists of day assignments with at least one duplicated dayNumber, verify 400 rejection
    - `@Property(tries = 100)`

  - [x] 9.5 Write property test: non-existent workout references are rejected
    - **Property 5: Non-existent workout references are rejected**
    - **Validates: Requirements 4.1, 4.2**
    - Generate valid commands with workout-type days referencing random UUIDs, mock repository to return false for `existsByIdAndOwner`, verify `WorkoutNotFoundException` is thrown
    - `@Property(tries = 100)`

  - [x] 9.6 Write property test: manual program persistence round-trip
    - **Property 6: Manual program persistence round-trip**
    - **Validates: Requirements 5.1, 5.2, 5.3**
    - Generate fully valid `CreateManualProgramCommand` instances, use in-memory repository stub, verify persisted program has same name, owner, contentSource=MANUAL, and equivalent day assignments
    - `@Property(tries = 100)`

- [x] 10. Write integration test for the full endpoint
  - [x] 10.1 Write integration test for `POST /api/v1/vault/programs`
    - Test class: `ManualProgramCreationIntegrationTest` in `src/test/java/.../workoutcreator/integration/vault/`
    - Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate`
    - Tests: valid body → 201 + UUID response, missing JWT → 401, malformed JSON → 400, blank program name → 400, missing workoutId for workout type → 400, non-existent workout reference → 400, created program appears in GET list
    - _Requirements: 1.1, 1.2, 2.1, 3.2, 4.1, 6.2, 6.3_

- [x] 11. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The implementation follows hexagonal architecture: domain → ports → application → adapters → tests
- The Flyway migration uses V103 as specified (next in the V100-V199 range after existing V102)
- Manual programs reuse the existing `programs` table with sentinel values (`duration_weeks=0`, `goal='Manual Program'`, `equipment_profile='[]'`)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "3.2"] },
    { "id": 1, "tasks": ["1.2", "2.1"] },
    { "id": 2, "tasks": ["2.2", "3.1"] },
    { "id": 3, "tasks": ["4.1", "6.1", "7.1", "7.2"] },
    { "id": 4, "tasks": ["4.2", "6.2"] },
    { "id": 5, "tasks": ["6.3", "7.3"] },
    { "id": 6, "tasks": ["7.4"] },
    { "id": 7, "tasks": ["9.1", "9.2", "9.3", "9.4", "9.5", "9.6"] },
    { "id": 8, "tasks": ["10.1"] }
  ]
}
```
