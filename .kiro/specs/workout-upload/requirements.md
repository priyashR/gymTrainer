# Requirements Document

## Introduction

The Workout Upload feature allows authenticated Users to import training content into their Vault by uploading a JSON file. A User may upload either a single Workout (one training session) or a Program (1–4 weeks of workouts). Uploaded content is owned by the uploading User and is subject to the same ownership and visibility rules as AI-generated Vault items. The feature spans the `workout-creator-service` backend and the `workout-coach-ui` frontend.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Workout**: A single training session definition containing one or more Sections
- **Program**: A structured collection of Workouts spanning 1 to 4 weeks
- **Section**: A named block within a Workout with a defined type (e.g., Strength, AMRAP, EMOM, Tabata, For Time, Accessory)
- **Exercise**: A single movement within a Section
- **Vault**: A User's personal library of saved Workouts and Programs
- **Upload_Parser**: The component responsible for deserialising and validating an uploaded JSON file into a domain object
- **Upload_Formatter**: The component responsible for serialising a Workout or Program domain object back into the canonical JSON upload format
- **Workout_Creator_Service**: The microservice responsible for Vault management, including upload ingestion and persistence
- **Workout_Coach_UI**: The React SPA frontend providing the file upload interface
- **Section_Type**: One of the six supported section classifications: `STRENGTH`, `AMRAP`, `EMOM`, `TABATA`, `FOR_TIME`, `ACCESSORY`
- **Upload_Schema**: The agreed JSON structure for Workout and Program upload files, defined in Requirement 1

---

## Requirements

### Requirement 1: Upload JSON Schema Definition

**User Story:** As a User, I want a well-defined JSON format for uploads, so that I can prepare valid files outside the platform and import them reliably.

#### Acceptance Criteria

1. THE Workout_Creator_Service SHALL accept Workout upload files conforming to the following schema:
   - `type`: string, required, value must be `"workout"`
   - `name`: string, required, non-empty
   - `description`: string, optional
   - `estimatedDurationMinutes`: integer ≥ 1, optional
   - `tags`: array of strings, optional, may be empty
   - `sections`: array, required, must contain at least one Section object
   - Each Section object must contain:
     - `name`: string, required, non-empty
     - `type`: string, required, one of `STRENGTH`, `AMRAP`, `EMOM`, `TABATA`, `FOR_TIME`, `ACCESSORY`
     - `durationSeconds`: integer ≥ 1, required when `type` is `AMRAP`, `EMOM`, `TABATA`, or `FOR_TIME`; omitted or null when `type` is `STRENGTH` or `ACCESSORY`
     - `exercises`: array, required, must contain at least one Exercise object
   - Each Exercise object must contain:
     - `name`: string, required, non-empty
     - `sets`: integer ≥ 1, optional
     - `reps`: string or integer, optional (e.g., `10` or `"10-12"`)
     - `weightKg`: number ≥ 0, optional
     - `restSeconds`: integer ≥ 0, optional
     - `notes`: string, optional

2. THE Workout_Creator_Service SHALL accept Program upload files conforming to the following schema:
   - `type`: string, required, value must be `"program"`
   - `name`: string, required, non-empty
   - `description`: string, optional
   - `weeks`: array, required, must contain between 1 and 4 Week objects
   - Each Week object must contain:
     - `weekNumber`: integer, required, value between 1 and 4 inclusive
     - `days`: array, required, must contain at least one Day object
   - Each Day object must contain:
     - `dayNumber`: integer, required, value between 1 and 7 inclusive
     - `workout`: object, required, containing a `sections` array conforming to the Section and Exercise schema defined in Acceptance Criterion 1 (without `type`, `name`, `description`, `estimatedDurationMinutes`, or `tags` fields)

3. THE Upload_Schema SHALL be the single authoritative definition used by both the Upload_Parser for validation and the Upload_Formatter for serialisation.

---

### Requirement 2: Workout File Upload — Backend

**User Story:** As a User, I want to upload a single Workout JSON file, so that I can add a training session I created externally to my Vault.

#### Acceptance Criteria

1. WHEN a User submits a POST request to `/api/v1/uploads/workouts` with a valid Workout JSON file not exceeding 1 MB, THE Workout_Creator_Service SHALL parse the file, validate it against the Workout upload schema, persist the resulting Workout to the User's Vault, and return a 201 Created response containing the persisted Workout including its assigned identifier.

2. WHEN a User submits a Workout upload request, THE Workout_Creator_Service SHALL associate the persisted Workout with the authenticated User's identity as resolved from the JWT subject claim; THE Workout_Creator_Service SHALL NOT use any client-supplied owner identifier.

3. WHEN a Workout upload request is received without a valid JWT, THE Workout_Creator_Service SHALL return a 401 Unauthorised response.

4. WHEN a Workout upload request contains a file exceeding 1 MB, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the message `"File size exceeds the maximum allowed limit of 1 MB"` before attempting to parse the file content.

5. WHEN a Workout upload request contains content that is not valid JSON, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the message `"Uploaded file is not valid JSON"`.

6. WHEN a Workout upload request contains valid JSON that fails schema validation, THE Workout_Creator_Service SHALL return a 400 Bad Request response containing an `errors` array where each entry identifies the failing field path and a descriptive message.

7. WHEN a Workout upload request contains a JSON object whose `type` field is not `"workout"`, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the message `"Expected type 'workout' but received '<actual_value>'"`.

---

### Requirement 3: Program File Upload — Backend

**User Story:** As a User, I want to upload a Program JSON file covering 1 to 4 weeks, so that I can import a structured training plan I created externally into my Vault.

#### Acceptance Criteria

1. WHEN a User submits a POST request to `/api/v1/uploads/programs` with a valid Program JSON file not exceeding 1 MB, THE Workout_Creator_Service SHALL parse the file, validate it against the Program upload schema, persist the resulting Program to the User's Vault, and return a 201 Created response containing the persisted Program including its assigned identifier.

2. WHEN a Program upload request is received, THE Workout_Creator_Service SHALL associate the persisted Program with the authenticated User's identity as resolved from the JWT subject claim; THE Workout_Creator_Service SHALL NOT use any client-supplied owner identifier.

3. WHEN a Program upload request is received without a valid JWT, THE Workout_Creator_Service SHALL return a 401 Unauthorised response.

4. WHEN a Program upload request contains a file exceeding 1 MB, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the message `"File size exceeds the maximum allowed limit of 1 MB"` before attempting to parse the file content.

5. WHEN a Program upload request contains content that is not valid JSON, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the message `"Uploaded file is not valid JSON"`.

6. WHEN a Program upload request contains valid JSON that fails schema validation, THE Workout_Creator_Service SHALL return a 400 Bad Request response containing an `errors` array where each entry identifies the failing field path and a descriptive message.

7. WHEN a Program upload request contains a `weeks` array with fewer than 1 or more than 4 Week objects, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the field error `"weeks: must contain between 1 and 4 weeks"`.

8. WHEN a Program upload request contains a JSON object whose `type` field is not `"program"`, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the message `"Expected type 'program' but received '<actual_value>'"`.

---

### Requirement 4: Upload Ownership and Vault Integration

**User Story:** As a User, I want uploaded content to appear in my Vault with the same access rules as my other content, so that I can manage it consistently.

#### Acceptance Criteria

1. WHEN a Workout or Program is successfully uploaded, THE Workout_Creator_Service SHALL persist it to the same Vault data store used for AI-generated and manually created content, with the authenticated User's identifier as the owner.

2. WHILE a Workout or Program has been uploaded by a User, THE Workout_Creator_Service SHALL return it in that User's Vault listing responses alongside AI-generated and manually created content.

3. IF a User attempts to read, modify, or delete an uploaded Workout or Program owned by a different User, THEN THE Workout_Creator_Service SHALL return a 403 Forbidden response.

4. THE Workout_Creator_Service SHALL NOT expose uploaded Workouts or Programs to any User other than the owner; uploaded content is private by default.

5. WHEN an uploaded Workout or Program is persisted, THE Workout_Creator_Service SHALL record the content source as `UPLOADED` to distinguish it from `AI_GENERATED` and `MANUAL` content, without affecting any existing Vault behaviour.

---

### Requirement 5: Upload Parser and Formatter (Round-Trip)

**User Story:** As a developer, I want the upload parser and formatter to be inverses of each other, so that content fidelity is guaranteed across serialisation boundaries.

#### Acceptance Criteria

1. THE Upload_Parser SHALL deserialise a valid Workout JSON upload file into a Workout domain object, mapping all fields defined in the Upload_Schema to their corresponding domain model fields.

2. THE Upload_Parser SHALL deserialise a valid Program JSON upload file into a Program domain object, mapping all fields defined in the Upload_Schema to their corresponding domain model fields.

3. THE Upload_Formatter SHALL serialise a Workout domain object into a JSON representation conforming to the Workout Upload_Schema.

4. THE Upload_Formatter SHALL serialise a Program domain object into a JSON representation conforming to the Program Upload_Schema.

5. FOR ALL valid Workout upload JSON objects, parsing then formatting then parsing SHALL produce a Workout domain object equivalent to the one produced by the first parse (round-trip property).

6. FOR ALL valid Program upload JSON objects, parsing then formatting then parsing SHALL produce a Program domain object equivalent to the one produced by the first parse (round-trip property).

---

### Requirement 6: Upload File Constraints

**User Story:** As a platform operator, I want upload requests to be bounded in size and type, so that the service is protected from oversized or malformed payloads.

#### Acceptance Criteria

1. THE Workout_Creator_Service SHALL enforce a maximum upload file size of 1 MB (1,048,576 bytes) for all upload endpoints; this limit SHALL be enforced before JSON parsing begins.

2. WHEN an upload request is received with a `Content-Type` other than `application/json`, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the message `"Content-Type must be application/json"`.

3. WHEN an upload request body is empty, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the message `"Request body must not be empty"`.

4. THE Workout_Creator_Service SHALL NOT persist any partial state when an upload fails validation; the operation SHALL be atomic — either the full Workout or Program is persisted, or nothing is.

---

### Requirement 7: Upload UI — File Picker and Feedback

**User Story:** As a User, I want a file picker in the UI that guides me through uploading a JSON file and clearly communicates the result, so that I can import content without needing to use the API directly.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL provide a file picker control that accepts only files with the `.json` extension; files of other types SHALL be rejected by the control before any upload request is made.

2. WHEN a User selects a `.json` file and confirms the upload, THE Workout_Coach_UI SHALL submit the file to the appropriate upload endpoint (`/api/v1/uploads/workouts` or `/api/v1/uploads/programs`) based on the `type` field detected in the file content.

3. WHEN the upload endpoint returns a 201 Created response, THE Workout_Coach_UI SHALL display a confirmation message identifying the uploaded item by name and providing a navigation action to view it in the Vault.

4. WHEN the upload endpoint returns a 400 Bad Request response with an `errors` array, THE Workout_Coach_UI SHALL display a field-level error summary listing each failing field path and its associated message.

5. WHEN the upload endpoint returns a 400 Bad Request response with a single `message` (non-JSON or size violation), THE Workout_Coach_UI SHALL display that message to the User.

6. WHEN the upload endpoint returns a 401 Unauthorised response, THE Workout_Coach_UI SHALL redirect the User to the login screen.

7. WHILE an upload request is in progress, THE Workout_Coach_UI SHALL display a loading indicator and SHALL disable the upload action to prevent duplicate submissions.

8. THE Workout_Coach_UI SHALL display the maximum allowed file size (1 MB) as a hint adjacent to the file picker control.

---

### Requirement 8: Upload API Error Response Consistency

**User Story:** As a developer integrating with the upload API, I want all error responses to follow the platform's standard error shape, so that error handling is consistent across the application.

#### Acceptance Criteria

1. THE Workout_Creator_Service SHALL return all upload error responses using the platform standard error shape: `status`, `error`, `message`, `path`, and `timestamp` fields.

2. WHEN an upload request produces multiple field-level validation errors, THE Workout_Creator_Service SHALL return a response containing an `errors` array where each entry has a `field` property using dot-notation for nested fields (e.g., `"sections[0].exercises[0].name"`) and a `message` property.

3. THE Workout_Creator_Service SHALL NOT include stack traces, internal class names, SQL, or internal identifiers in any upload error response.

