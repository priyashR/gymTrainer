# Requirements Document

## Introduction

This feature adds a `POST /api/v1/vault/programs` endpoint to the workout-creator-service that allows authenticated users to manually create a training program by assigning existing vault workouts or external activities to numbered training days. The frontend "Create Program" screen is already built and sends a structured JSON body with a program name and an ordered list of day assignments. The backend must validate, persist, and return the created program's identifier.

## Glossary

- **Manual_Program**: A user-created program consisting of a name and an ordered list of day assignments that reference existing vault workouts or external activities.
- **Day_Assignment**: A single entry within a Manual_Program that maps a day number to either a vault workout (by ID) or an external activity (by type name).
- **Vault_Workout**: An existing workout record in the user's vault, identified by a UUID. Persisted in the `programs` table with `content_source` of AI_GENERATED, UPLOADED, or MANUAL.
- **External_Activity**: A non-workout training activity (e.g., Soccer, Swimming) that does not reference a stored workout.
- **Endpoint**: The REST controller method at `POST /api/v1/vault/programs` that accepts the creation request.
- **Create_Program_Service**: The application-layer use case responsible for orchestrating validation and persistence of a Manual_Program.
- **Program_Repository**: The outbound port responsible for persisting Manual_Program records to the database.

## Requirements

### Requirement 1: Accept Manual Program Creation Request

**User Story:** As a user, I want to submit a program name and day assignments via the Create Program screen, so that my manually assembled program is saved to my vault.

#### Acceptance Criteria

1. WHEN an authenticated user sends a POST request to `/api/v1/vault/programs` with a valid JSON body containing `programName` and `days`, THE Endpoint SHALL deserialise the request body into a creation command and delegate to the Create_Program_Service.
2. WHEN the Create_Program_Service successfully persists the Manual_Program, THE Endpoint SHALL return HTTP 201 Created with a JSON body containing `{ "id": "<program-uuid>" }`.
3. THE Endpoint SHALL resolve the owning user's identity from the JWT subject claim and never from client-supplied data.

### Requirement 2: Validate Program Name

**User Story:** As a user, I want to be informed immediately if I forget to provide a program name, so that I can correct the input before submission.

#### Acceptance Criteria

1. WHEN the `programName` field is null, empty, or composed entirely of whitespace, THE Endpoint SHALL return HTTP 400 Bad Request with a descriptive validation error message.
2. WHEN the `programName` field exceeds 255 characters, THE Endpoint SHALL return HTTP 400 Bad Request with a descriptive validation error message.

### Requirement 3: Validate Day Assignments

**User Story:** As a user, I want the system to reject programs that have no day assignments, so that I cannot accidentally save a meaningless empty program.

#### Acceptance Criteria

1. WHEN the `days` array is null or empty, THE Endpoint SHALL return HTTP 400 Bad Request with a message indicating at least one day assignment is required.
2. WHEN a day assignment has `type` equal to `"workout"` but `workoutId` is null or blank, THE Endpoint SHALL return HTTP 400 Bad Request indicating the workout ID is required for workout-type days.
3. WHEN a day assignment has `type` equal to `"activity"` but `activityType` is null or blank, THE Endpoint SHALL return HTTP 400 Bad Request indicating the activity type is required for activity-type days.
4. WHEN a day assignment has a `dayNumber` that is less than 1, THE Endpoint SHALL return HTTP 400 Bad Request with a message indicating the day number must be positive.
5. WHEN duplicate `dayNumber` values exist in the `days` array, THE Endpoint SHALL return HTTP 400 Bad Request indicating day numbers must be unique.

### Requirement 4: Verify Referenced Workouts Exist in User's Vault

**User Story:** As a user, I want the system to ensure that all workouts I reference in my program actually exist in my vault, so that I do not end up with a broken program referencing deleted or non-existent workouts.

#### Acceptance Criteria

1. WHEN a day assignment references a `workoutId` that does not exist in the authenticated user's vault, THE Create_Program_Service SHALL return HTTP 400 Bad Request with a message identifying which workout ID was not found.
2. WHEN a day assignment references a `workoutId` that exists in another user's vault but not the authenticated user's vault, THE Create_Program_Service SHALL return HTTP 400 Bad Request (not 403 or 404) to avoid leaking resource existence.

### Requirement 5: Persist Manual Program

**User Story:** As a user, I want my manually created program to be durably stored in the database, so that I can retrieve, update, or use it later.

#### Acceptance Criteria

1. WHEN the Create_Program_Service receives a valid creation command, THE Program_Repository SHALL persist a new Manual_Program record with a generated UUID, the authenticated user's ID as owner, content source set to MANUAL, and the current timestamp for both `createdAt` and `updatedAt`.
2. THE Program_Repository SHALL persist each Day_Assignment with its `dayNumber`, `type`, `workoutId` (for workout-type days), and `activityType` (for activity-type days) linked to the parent Manual_Program record.
3. FOR ALL persisted Manual_Program records, reading the record back by its generated ID SHALL produce an equivalent set of day assignments to those originally submitted (round-trip consistency).

### Requirement 6: Error Handling

**User Story:** As a user, I want clear and consistent error messages when my program creation request fails, so that I can understand and correct the issue.

#### Acceptance Criteria

1. IF an unexpected database error occurs during persistence, THEN THE Create_Program_Service SHALL return HTTP 500 Internal Server Error with a generic message that does not expose internal details.
2. IF the request body is malformed JSON, THEN THE Endpoint SHALL return HTTP 400 Bad Request with a message indicating the body could not be parsed.
3. IF the request lacks a valid JWT, THEN THE Endpoint SHALL return HTTP 401 Unauthorised via Spring Security's built-in filter chain.
