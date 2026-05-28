# Requirements Document — Workout Session Service: Performance Tracking

## Introduction

This spec covers live performance logging during active workout sessions. The existing workout-session-service already supports Theater Mode with exercise checkoff, rest timers, section navigation, and session lifecycle (start/pause/end). What is missing is the ability to log actual performance data — weight, reps, RPE for strength sets, and CrossFit scores (rounds, additional reps, total time) for AMRAP, EMOM, and For Time sections — and to include that data in the SessionCompleted event published to RabbitMQ.

This feature extends the existing `ExerciseLog` domain object (currently limited to exerciseIndex, exerciseName, completed, completedAt) with structured performance fields, adds a `FOR_TIME` section type to the existing enum, introduces a tap-based round counter UI for AMRAP sections, and enriches the SessionCompleted event payload with performance numbers for downstream consumption by the Progress Tracker Service.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Session**: A single in-progress or completed execution of a Workout by a User
- **Session_Service**: The Spring Boot microservice (port 8083) responsible for active Workout execution and Session state management
- **Workout_Coach_UI**: The React 18 SPA frontend serving all user-facing views
- **ExerciseLog**: The domain object tracking completion state and performance data for a single exercise within a section
- **SectionProgress**: The domain object tracking progress of a single section within a session, containing a list of ExerciseLog entries
- **Set_Log**: A record of a single set performed within a Strength Exercise, capturing weight, repetitions, and RPE
- **RPE**: Rate of Perceived Exertion — a subjective effort scale from 1 to 10 (half-point increments allowed, e.g. 7.5)
- **CrossFit_Score**: A composite result recorded for AMRAP, EMOM, or For Time sections, comprising rounds completed, additional reps, and total time where applicable
- **AMRAP**: As Many Rounds As Possible — a timed Section type using a countdown timer
- **EMOM**: Every Minute On the Minute — an interval Section type
- **FOR_TIME**: A Section type where the User completes a fixed amount of work as fast as possible, recording total elapsed time
- **Round_Counter**: A tap-based UI component displayed during active AMRAP sections that increments the round count with each tap
- **SessionCompleted**: A domain event published to RabbitMQ when a User completes a Session, consumed by the Progress Tracker Service
- **Progress_Tracker_Service**: The downstream microservice that consumes SessionCompleted events for analytics and benchmarking
- **Theater_Mode**: The distraction-free active workout execution UI at route `/workout/session/:sessionId`

---

## Requirements

### Requirement 1: Strength Set Performance Logging

**User Story:** As a User, I want to log the weight, reps, and RPE for each set of a strength exercise, so that my training load is captured accurately for progress tracking.

#### Acceptance Criteria

1. WHEN a User completes a Strength Exercise set, THE Session_Service SHALL accept and persist the weight lifted (in kilograms, as a positive decimal), the number of repetitions (as a positive integer), and the RPE value (as a decimal between 1.0 and 10.0 inclusive, in 0.5 increments) for that set.
2. THE Session_Service SHALL associate each Set_Log with the correct ExerciseLog within the correct SectionProgress of the active Session.
3. THE Session_Service SHALL record a timestamp (ISO-8601 with timezone) on each Set_Log at the moment it is persisted, capturing when the set was completed.
4. WHILE a Session is active and the current Section is of type STRENGTH, THE Workout_Coach_UI SHALL display input fields for weight, repetitions, and RPE alongside each set of each Exercise in the Exercise_Checklist.
5. WHEN a User submits a set log, THE Workout_Coach_UI SHALL send the set data to the Session_Service and SHALL visually confirm persistence by marking the set as recorded.
6. THE Session_Service SHALL allow a User to log multiple sets per Exercise within a single Strength Section, with each set stored as a separate Set_Log entry in chronological order.
7. IF a User submits a set log with weight less than or equal to zero, repetitions less than or equal to zero, or RPE outside the range 1.0 to 10.0, THEN THE Session_Service SHALL reject the request with a validation error and SHALL NOT persist the invalid data.
8. THE Session_Service SHALL allow RPE to be omitted (nullable) when logging a set, treating it as an optional field.

---

### Requirement 2: CrossFit Score Logging

**User Story:** As a User, I want to log my CrossFit scores (rounds, additional reps, and time) for AMRAP, EMOM, and For Time sections, so that my conditioning performance is tracked alongside my strength work.

#### Acceptance Criteria

1. WHEN a User completes an AMRAP Section, THE Session_Service SHALL accept and persist the CrossFit_Score containing rounds completed (non-negative integer) and additional reps (non-negative integer).
2. WHEN a User completes an EMOM Section, THE Session_Service SHALL accept and persist the CrossFit_Score containing rounds completed (non-negative integer) and additional reps (non-negative integer).
3. WHEN a User completes a For Time Section, THE Session_Service SHALL accept and persist the CrossFit_Score containing rounds completed (non-negative integer), additional reps (non-negative integer), and total time in seconds (positive integer).
4. THE Session_Service SHALL associate each CrossFit_Score with the correct SectionProgress of the active Session.
5. THE Session_Service SHALL record a timestamp (ISO-8601 with timezone) on each CrossFit_Score at the moment it is persisted, capturing when the score was logged.
6. IF a User submits a CrossFit_Score with rounds less than zero, additional reps less than zero, or total time less than or equal to zero (for For Time sections), THEN THE Session_Service SHALL reject the request with a validation error.
7. THE Session_Service SHALL allow only one CrossFit_Score per Section per Session; submitting a new score for a Section that already has one SHALL overwrite the previous score.
8. WHILE a Session is active and the current Section is of type AMRAP, EMOM, or FOR_TIME, THE Workout_Coach_UI SHALL display score input fields appropriate to the Section type upon Section completion.

---

### Requirement 3: AMRAP Round Counter UI

**User Story:** As a User performing an AMRAP workout, I want a tap-based round counter so that I can quickly track rounds during high-intensity work without typing numbers.

#### Acceptance Criteria

1. DURING an active AMRAP Section, THE Workout_Coach_UI SHALL display a Round_Counter component with a prominent tap target that increments the displayed round count by one with each tap.
2. THE Round_Counter SHALL display the current round count as a large, easily readable number.
3. THE Workout_Coach_UI SHALL provide a decrement control on the Round_Counter to correct accidental taps, reducing the count by one (minimum zero).
4. WHEN the AMRAP timer expires or the User manually ends the Section, THE Workout_Coach_UI SHALL display a field for the User to enter additional reps completed in the final partial round.
5. WHEN the User confirms the AMRAP score (rounds from the counter plus additional reps), THE Workout_Coach_UI SHALL submit the CrossFit_Score to the Session_Service for persistence.
6. THE Round_Counter SHALL persist its current count in the Session state so that the count is preserved if the User navigates away from the Section and returns.

---

### Requirement 4: SessionCompleted Event Enrichment

**User Story:** As a platform operator, I want the SessionCompleted event to include all logged performance data, so that the Progress Tracker Service can compute analytics without additional API calls.

#### Acceptance Criteria

1. WHEN a User ends a Session, THE Session_Service SHALL include all Set_Log entries (weight, repetitions, RPE) for each ExerciseLog within the SessionCompleted event payload.
2. WHEN a User ends a Session, THE Session_Service SHALL include all CrossFit_Score entries (rounds, additional reps, total time) for each SectionProgress within the SessionCompleted event payload.
3. THE SessionCompleted event payload SHALL maintain backward compatibility by preserving all existing fields (eventId, occurredAt, userId, sessionId, programId, weekNumber, dayNumber, standalone, sectionProgresses, startedAt, completedAt) alongside the new performance data.
4. IF a Session is ended with no performance data logged for a given exercise or section, THEN THE Session_Service SHALL include that exercise or section in the event with empty performance fields (empty set logs list, null CrossFit_Score) rather than omitting it.

---

### Requirement 5: Empty Session Completion Guard

**User Story:** As a User, I want to be warned before ending a session with no logged data, so that I do not accidentally discard a workout without recording any performance.

#### Acceptance Criteria

1. IF a User attempts to end a Session with no logged performance data (zero Set_Log entries across all Strength sections AND zero CrossFit_Score entries across all scored sections), THEN THE Workout_Coach_UI SHALL display a confirmation prompt informing the User that no performance data has been recorded.
2. WHEN the User confirms the empty-session prompt, THE Session_Service SHALL mark the Session as complete and publish the SessionCompleted event as normal.
3. WHEN the User cancels the empty-session prompt, THE Workout_Coach_UI SHALL return the User to the active Session without ending it.
4. IF a Session has at least one Set_Log entry or one CrossFit_Score entry, THEN THE Workout_Coach_UI SHALL proceed with Session completion without displaying the empty-session prompt.

---

### Requirement 6: Exercise Prescription Display

**User Story:** As a User, I want to see the prescribed sets, reps, weight, and notes for each exercise during my workout, so that I know exactly what to do without referring to a separate plan.

#### Acceptance Criteria

1. WHILE a Session is active, THE Workout_Coach_UI SHALL display the prescribed number of sets and prescribed reps for each Exercise in the current Section's Exercise_Checklist.
2. WHILE a Session is active and the Exercise definition includes a prescribed weight, THE Workout_Coach_UI SHALL display the prescribed weight alongside the exercise name.
3. WHILE a Session is active and the Exercise definition includes notes (e.g., tempo, cues, modifications), THE Workout_Coach_UI SHALL display the notes beneath the exercise name in a secondary text style.
4. WHILE a Session is active and the current Section has a time cap (AMRAP, FOR_TIME), THE Workout_Coach_UI SHALL display the time cap prominently alongside the section name.
5. WHILE a Session is active and the current Section has a format descriptor (e.g., "5 rounds for time", "20 min AMRAP"), THE Workout_Coach_UI SHALL display the format descriptor alongside the section name.
6. THE Workout_Coach_UI SHALL NOT display fields that are absent from the Exercise or Section definition (e.g., if no prescribed weight exists, no weight placeholder is shown).

---

### Requirement 7: Session Timing and Duration

**User Story:** As a User, I want to see when my workout started, when it ended, and how long it took, so that I can track my training duration over time.

#### Acceptance Criteria

1. WHEN a Session is started, THE Session_Service SHALL record the start time (ISO-8601 with timezone) and persist it with the Session.
2. WHEN a Session is ended, THE Session_Service SHALL record the end time (ISO-8601 with timezone) and persist it with the Session.
3. WHEN a Session is ended, THE Session_Service SHALL compute and persist the total duration in seconds as the difference between end time and start time, excluding any time spent in PAUSED state.
4. WHILE a Session is active, THE Workout_Coach_UI SHALL display a running elapsed time indicator showing how long the workout has been in progress (excluding paused time).
5. WHEN a Session is ended, THE Workout_Coach_UI SHALL display the total workout duration in a human-readable format (e.g., "45 min", "1h 12 min").
6. THE SessionCompleted event SHALL include the start time, end time, and total duration (in seconds, excluding paused time) in its payload.

---
