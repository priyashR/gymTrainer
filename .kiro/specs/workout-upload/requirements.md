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
- **Upload_Schema**: The agreed JSON structure for Program upload files, defined in Requirement 1
- **Block**: A named training segment within a Day (e.g., Tier 1: Compound, Metcon, Finisher), replacing the previously named "Section"
- **Movement**: A single exercise entry within a Block, equivalent to "Exercise" in the domain model
- **Modality**: The training style of a Day or Movement — either `CrossFit` or `Hypertrophy`
- **Modality_Type**: The CrossFit movement classification — one of `Engine`, `Gymnastics`, or `Weightlifting`; only applicable when the Day modality is `CrossFit`
- **program_metadata**: The top-level object describing the program's identity, goal, and equipment requirements
- **program_structure**: The ordered list of weeks and days that make up the program content

---

## Requirements

### Requirement 1: Upload JSON Schema Definition

**User Story:** As a User, I want a well-defined JSON format for uploads, so that I can prepare valid files outside the platform and import them reliably.

#### Acceptance Criteria

1. THE Workout_Creator_Service SHALL accept Program upload files conforming to the following canonical JSON schema:

```json
{
  "program_metadata": {
    "program_name": "String (required, non-empty)",
    "duration_weeks": "Integer (required, value 1 or 4)",
    "goal": "String (required, e.g. 'Hypertrophy', 'GPP', 'Strength Bias')",
    "equipment_profile": ["String (required, at least one entry)"],
    "version": "String (required, value '1.0')"
  },
  "program_structure": [
    {
      "week_number": "Integer (required, 1–4, must match duration_weeks)",
      "days": [
        {
          "day_number": "Integer (required, 1–7)",
          "day_label": "String (required, e.g. 'Monday')",
          "focus_area": "String (required, e.g. 'Push', 'Pull', 'Metcon', 'Full Body')",
          "modality": "String (required, one of: 'CrossFit', 'Hypertrophy')",
          "warm_up": [
            {
              "movement": "String (required, non-empty)",
              "instruction": "String (required, non-empty)"
            }
          ],
          "blocks": [
            {
              "block_type": "String (required, e.g. 'Tier 1: Compound', 'Strength Bias', 'Metcon', 'Tier 3: Finisher')",
              "format": "String (required, e.g. 'Sets/Reps', 'AMRAP', 'EMOM', 'RFT')",
              "time_cap_minutes": "Integer (optional, ≥ 1)",
              "movements": [
                {
                  "exercise_name": "String (required, non-empty)",
                  "modality_type": "String (optional, one of: 'Engine', 'Gymnastics', 'Weightlifting' — required when day modality is 'CrossFit')",
                  "prescribed_sets": "Integer (required, ≥ 1)",
                  "prescribed_reps": "String (required, e.g. '8-10' or 'Max')",
                  "prescribed_weight": "String (optional, e.g. '75% 1RM' or '22.5kg')",
                  "rest_interval_seconds": "Integer (optional, ≥ 0)",
                  "notes": "String (optional)"
                }
              ]
            }
          ],
          "cool_down": [
            {
              "movement": "String (required, non-empty)",
              "instruction": "String (required, non-empty)"
            }
          ],
          "methodology_source": "String (optional, e.g. 'Inspired by: Renaissance Periodization')"
        }
      ]
    }
  ]
}
```

2. THE Upload_Schema field constraints SHALL be enforced as follows:
   - `program_metadata.duration_weeks` must be exactly `1` or `4`
   - `program_metadata.version` must be exactly `"1.0"`
   - `program_metadata.equipment_profile` must contain at least one non-empty string
   - `program_structure` must contain a number of week entries equal to `program_metadata.duration_weeks`
   - Each `week_number` must be unique within `program_structure` and fall within the range 1 to `duration_weeks`
   - Each `day_number` must be unique within its week and fall within the range 1–7
   - Each `blocks` array must contain at least one Block object
   - Each `movements` array must contain at least one Movement object
   - `modality_type` is required on each Movement when the parent Day's `modality` is `"CrossFit"`; it is optional when `modality` is `"Hypertrophy"`

3. THE Upload_Schema SHALL be the single authoritative definition used by both the Upload_Parser for validation and the Upload_Formatter for serialisation.

4. THE Workout_Creator_Service SHALL use a single upload endpoint `/api/v1/uploads/programs` for all program uploads; there is no separate single-workout upload endpoint — a single-week program (`duration_weeks: 1`) is the canonical representation of a standalone workout day collection.

---

### Requirement 2: Program File Upload — Backend

**User Story:** As a User, I want to upload a Program JSON file, so that I can import a structured training plan I created externally into my Vault.

#### Acceptance Criteria

1. WHEN a User submits a POST request to `/api/v1/uploads/programs` with a valid Program JSON file not exceeding 1 MB, THE Workout_Creator_Service SHALL parse the file, validate it against the Upload_Schema, persist the resulting Program to the User's Vault, and return a 201 Created response containing the persisted Program including its assigned identifier and `program_metadata.program_name`.

2. WHEN a Program upload request is received, THE Workout_Creator_Service SHALL associate the persisted Program with the authenticated User's identity as resolved from the JWT subject claim; THE Workout_Creator_Service SHALL NOT use any client-supplied owner identifier.

3. WHEN a Program upload request is received without a valid JWT, THE Workout_Creator_Service SHALL return a 401 Unauthorised response.

4. WHEN a Program upload request contains a file exceeding 1 MB, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the message `"File size exceeds the maximum allowed limit of 1 MB"` before attempting to parse the file content.

5. WHEN a Program upload request contains content that is not valid JSON, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the message `"Uploaded file is not valid JSON"`.

6. WHEN a Program upload request contains valid JSON that fails schema validation, THE Workout_Creator_Service SHALL return a 400 Bad Request response containing an `errors` array where each entry identifies the failing field path and a descriptive message.

7. WHEN a Program upload request contains a `program_structure` array whose length does not match `program_metadata.duration_weeks`, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the field error `"program_structure: number of weeks does not match duration_weeks"`.

8. WHEN a Program upload request contains a `program_metadata.duration_weeks` value other than `1` or `4`, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the field error `"program_metadata.duration_weeks: must be 1 or 4"`.

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

1. THE Upload_Parser SHALL deserialise a valid Program JSON upload file into a Program domain object, mapping all fields defined in the Upload_Schema to their corresponding domain model fields, including `program_metadata`, all weeks, days, blocks, movements, warm-up entries, and cool-down entries.

2. THE Upload_Formatter SHALL serialise a Program domain object into a JSON representation conforming to the Upload_Schema, including all `program_metadata` fields and the full `program_structure`.

3. FOR ALL valid Program upload JSON objects, parsing then formatting then parsing SHALL produce a Program domain object equivalent to the one produced by the first parse (round-trip property).

4. THE round-trip property SHALL hold for programs with `duration_weeks` of both `1` and `4`, and for days with both `CrossFit` and `Hypertrophy` modalities.

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

2. WHEN a User selects a `.json` file and confirms the upload, THE Workout_Coach_UI SHALL submit the file to `/api/v1/uploads/programs`.

3. WHEN the upload endpoint returns a 201 Created response, THE Workout_Coach_UI SHALL display a confirmation message identifying the uploaded program by its `program_metadata.program_name` and providing a navigation action to view it in the Vault.

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

