# Requirements Document

## Introduction

Theater Mode Recommendations provides real-time exercise prescription guidance during an active workout session. For hypertrophy exercises, the system displays recommended weight, repetitions, and sets based on the workout definition stored in the workout snapshot. For CrossFit workout exercises, the system displays recommended weights. These recommendations are surfaced in the Theater Mode UI to help users execute their prescribed workout without needing to recall details from memory.

## Glossary

- **Recommendation_Engine**: The domain component within the workout-session-service that computes exercise recommendations from the workout snapshot and session context.
- **Theater_Mode**: The real-time workout execution view in the workout-coach-ui where users perform exercises, log sets, and see progress.
- **Workout_Snapshot**: The JSON representation of the workout definition stored on the Session aggregate, containing sections, exercises, and their prescribed parameters (sets, reps, weight).
- **Hypertrophy_Exercise**: An exercise belonging to a section within a day whose modality is HYPERTROPHY. These exercises have prescribed sets, reps, and weight.
- **CrossFit_Exercise**: An exercise belonging to a section within a day whose modality is CROSSFIT. These exercises have a prescribed weight but reps and sets are determined by the workout format (AMRAP, EMOM, FOR_TIME).
- **Prescribed_Weight**: The weight value defined in the Exercise within the workout snapshot (nullable String field, e.g. "80kg", "60% 1RM", "bodyweight").
- **Prescribed_Reps**: The repetitions value defined in the Exercise within the workout snapshot (String field, e.g. "8-10", "12", "AMRAP").
- **Prescribed_Sets**: The integer sets value defined in the Exercise within the workout snapshot.
- **Exercise_Recommendation**: A value object containing the recommended weight, reps, and sets for a specific exercise within the current session.
- **Session_Service**: The workout-session-service Spring Boot backend that owns session state and serves Theater Mode data.
- **Theater_UI**: The React components in the workout-coach-ui that render the Theater Mode interface.

## Requirements

### Requirement 1: Extract Recommendations from Workout Snapshot

**User Story:** As a user performing a workout in Theater Mode, I want to see the prescribed weight, reps, and sets for each exercise, so that I know exactly what to lift without checking my program externally.

#### Acceptance Criteria

1. WHILE a Session has status IN_PROGRESS or PAUSED, WHEN the Recommendation_Engine is invoked for a given section index and exercise index, THE Recommendation_Engine SHALL extract the Prescribed_Weight, Prescribed_Reps, and Prescribed_Sets from the Workout_Snapshot's corresponding section and exercise entry.
2. WHEN the Workout_Snapshot contains a null Prescribed_Weight for an exercise, THE Recommendation_Engine SHALL return an Exercise_Recommendation with a null weight field.
3. THE Recommendation_Engine SHALL identify exercises by matching section index and exercise index between the Session's SectionProgress list and the Workout_Snapshot sections array.
4. IF the requested section index or exercise index does not have a corresponding entry in the Workout_Snapshot sections array, THEN THE Recommendation_Engine SHALL return an Exercise_Recommendation with all fields null for that exercise.

### Requirement 2: Hypertrophy Exercise Recommendations

**User Story:** As a user performing a hypertrophy workout, I want to see the recommended weight, reps, and sets for each exercise, so that I can follow my program's progressive overload plan.

#### Acceptance Criteria

1. WHEN the Day modality is HYPERTROPHY, THE Recommendation_Engine SHALL populate the Exercise_Recommendation with the Prescribed_Weight, Prescribed_Reps, and Prescribed_Sets fields extracted from the Workout_Snapshot for that exercise, where each field is either the snapshot value or null if the snapshot value is absent.
2. WHEN a HYPERTROPHY exercise has a Prescribed_Reps value containing a range (e.g. "8-10"), THE Recommendation_Engine SHALL preserve the range string as-is in the Exercise_Recommendation without interpreting it.
3. WHEN a HYPERTROPHY exercise has a Prescribed_Sets value of zero, THE Recommendation_Engine SHALL treat the sets field as unspecified and return null for Prescribed_Sets in the Exercise_Recommendation.
4. WHEN a HYPERTROPHY exercise has a Prescribed_Sets value greater than zero, THE Recommendation_Engine SHALL return that integer value as-is in the Exercise_Recommendation Prescribed_Sets field.

### Requirement 3: CrossFit Exercise Recommendations

**User Story:** As a user performing a CrossFit workout, I want to see the recommended weight for each exercise, so that I can load the barbell or select the correct equipment before starting the section.

#### Acceptance Criteria

1. WHEN the Day modality is CROSSFIT, THE Recommendation_Engine SHALL include only the Prescribed_Weight in the Exercise_Recommendation and SHALL set Prescribed_Reps and Prescribed_Sets to null.
2. IF a CROSSFIT exercise has a null Prescribed_Weight, THEN THE Recommendation_Engine SHALL return an Exercise_Recommendation with all fields (Prescribed_Weight, Prescribed_Reps, Prescribed_Sets) null.
3. WHEN a CROSSFIT exercise has a non-blank Prescribed_Weight string, THE Recommendation_Engine SHALL preserve the weight string as-is in the Exercise_Recommendation without transformation.

### Requirement 4: Serve Recommendations via REST API

**User Story:** As the Theater Mode UI, I want to retrieve exercise recommendations for the active session through a REST endpoint, so that I can display them alongside the exercise log interface.

#### Acceptance Criteria

1. THE Session_Service SHALL expose a GET endpoint at `/api/v1/sessions/{sessionId}/recommendations` that returns the list of Exercise_Recommendations for all exercises in the session, where each recommendation includes the section index, exercise index, Prescribed_Weight, Prescribed_Reps, and Prescribed_Sets.
2. IF a valid session ID and authenticated owner are provided, THEN THE Session_Service SHALL return a 200 OK response containing an array of recommendations grouped by section index, where each group contains the section index and an ordered array of Exercise_Recommendations for that section's exercises.
3. IF the session ID does not correspond to an existing session, THEN THE Session_Service SHALL return a 404 Not Found response.
4. IF the authenticated user does not own the session, THEN THE Session_Service SHALL return a 403 Forbidden response.
5. IF the Workout_Snapshot cannot be parsed, THEN THE Session_Service SHALL return a 500 Internal Server Error with a message indicating snapshot corruption.
6. IF the session exists but contains no exercises in the Workout_Snapshot, THEN THE Session_Service SHALL return a 200 OK response with an empty array.
7. THE Session_Service SHALL return the recommendations response within 2 seconds under normal operating conditions.

### Requirement 5: Deliver Recommendations via WebSocket

**User Story:** As a user in Theater Mode, I want recommendations to be included in the real-time session state pushed over WebSocket, so that I see prescriptions immediately without a separate API call.

#### Acceptance Criteria

1. WHEN the Session_Service publishes a session state update over STOMP to `/topic/sessions/{sessionId}`, THE Session_Service SHALL include a `recommendations` field in the message payload containing the list of Exercise_Recommendations for the current section only, identified by the session's current section index.
2. WHEN the user advances to a new section, THE Session_Service SHALL publish an updated state message containing the Exercise_Recommendations for the newly active section within the `recommendations` field.
3. THE Session_Service SHALL include recommendations only for the current section in each WebSocket message, excluding recommendations for all other sections.
4. IF the Recommendation_Engine fails to compute recommendations for the current section during a WebSocket push, THEN THE Session_Service SHALL publish the session state update with the `recommendations` field set to an empty list and SHALL NOT suppress the entire state message.

### Requirement 6: Display Recommendations in Theater Mode UI

**User Story:** As a user, I want to see the recommended weight, reps, and sets displayed clearly next to each exercise in Theater Mode, so that I can quickly reference my targets while training.

#### Acceptance Criteria

1. WHEN the Theater_UI renders an exercise card for a HYPERTROPHY exercise, THE Theater_UI SHALL display the Prescribed_Weight, Prescribed_Reps, and Prescribed_Sets in a recommendation badge, presented in the format "weight · reps · sets" (e.g., "80kg · 8-10 · 4").
2. WHEN the Theater_UI renders an exercise card for a CROSSFIT exercise, THE Theater_UI SHALL display only the Prescribed_Weight in the recommendation badge.
3. IF an Exercise_Recommendation has all three fields (Prescribed_Weight, Prescribed_Reps, and Prescribed_Sets) null, THEN THE Theater_UI SHALL not render the recommendation badge for that exercise.
4. IF an Exercise_Recommendation has a null Prescribed_Weight but at least one of Prescribed_Reps or Prescribed_Sets is non-null, THEN THE Theater_UI SHALL display only the non-null fields in the recommendation badge, omitting null fields without placeholder text.
5. THE Theater_UI SHALL render recommendation values as read-only text positioned outside the set-logging input controls such that the recommendation badge does not overlap, obscure, or disable any input element used for logging sets.
6. WHILE the Theater_UI is fetching Exercise_Recommendation data for the current section, THE Theater_UI SHALL display a loading indicator in place of the recommendation badge until data is available or an error occurs.
7. IF the Theater_UI fails to retrieve Exercise_Recommendation data for the current section, THEN THE Theater_UI SHALL hide the recommendation badge area for all exercises in that section and SHALL not block the user from logging sets.

### Requirement 7: Recommendation Value Object Integrity

**User Story:** As a developer, I want the Exercise_Recommendation value object to enforce domain invariants, so that invalid recommendations cannot propagate through the system.

#### Acceptance Criteria

1. THE Recommendation_Engine SHALL produce an Exercise_Recommendation where Prescribed_Sets is either null or an integer between 1 and 100 inclusive.
2. THE Recommendation_Engine SHALL produce an Exercise_Recommendation where Prescribed_Reps is either null or a non-blank string of at most 50 characters (where non-blank means containing at least one non-whitespace character).
3. THE Recommendation_Engine SHALL produce an Exercise_Recommendation where Prescribed_Weight is either null or a non-blank string of at most 50 characters (where non-blank means containing at least one non-whitespace character).
4. IF an Exercise_Recommendation is constructed with a Prescribed_Sets value that is zero or negative, or a Prescribed_Reps or Prescribed_Weight value that is blank, THEN THE Recommendation_Engine SHALL throw an IllegalArgumentException before the object is created.
5. THE Recommendation_Engine SHALL allow an Exercise_Recommendation where all three fields (Prescribed_Sets, Prescribed_Reps, Prescribed_Weight) are null simultaneously.
6. FOR ALL valid Workout_Snapshots, THE Recommendation_Engine SHALL produce Exercise_Recommendations such that serializing the list to JSON and deserializing it back yields a list where each Exercise_Recommendation has identical field values (null equality for null fields, value equality for non-null fields) to the original.
