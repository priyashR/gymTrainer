# Requirements Document — Progress Tracker Service

## Introduction

The Progress Tracker Service is responsible for performance analytics, progress dashboards, CrossFit benchmark tracking, and muscle activation heat maps. It consumes SessionCompleted events from RabbitMQ and maintains its own read-optimised Performance data store.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Session**: A single completed execution of a Workout by a User
- **Performance Log**: A record of a User's results for a completed Session
- **1RM**: One-repetition maximum — the maximum weight a User can lift for a single repetition of an Exercise
- **RPE**: Rate of Perceived Exertion — a subjective effort scale (1–10)
- **AMRAP**: As Many Rounds As Possible — a timed Section type
- **EMOM**: Every Minute On the Minute — an interval Section type
- **For Time**: A Section type where the User completes a fixed amount of work as fast as possible
- **CrossFit Score**: A result recorded for AMRAP (rounds + reps), EMOM, Tabata, or For Time Sections
- **PR**: Personal Record — the best result a User has achieved for a given Exercise or Workout
- **Benchmark Workout**: A named, standardised Workout used to measure fitness (e.g., "Fran", "Murph")
- **Progress_Tracker_Service**: The microservice responsible for performance analytics and dashboards
- **Workout_Coach_UI**: The single frontend application serving all user-facing views

---

## Requirements

### Requirement 1: Progress Dashboard

**User Story:** As a User, I want a performance dashboard, so that I can understand my training trends and results at a glance.

#### Acceptance Criteria

1. WHEN a User navigates to the Progress section, THE Progress_Tracker_Service SHALL return a dashboard summary including: total Sessions completed, total volume lifted (kg), total training time, and number of PRs achieved in the last 30 days.
2. THE Progress_Tracker_Service SHALL calculate and expose the estimated 1RM for any Exercise with sufficient logged data, using the Epley formula (weight × (1 + reps / 30)).
3. WHEN a User views Exercise stats for a specific Exercise, THE Progress_Tracker_Service SHALL return a chronological history of logged sets including date, weight, reps, RPE, and estimated 1RM.
4. WHEN a User views Workout stats for a specific Workout, THE Progress_Tracker_Service SHALL return a chronological history of Sessions for that Workout including date, total volume, and CrossFit Scores where applicable.
5. WHEN a User's logged performance for an Exercise exceeds their previous best weight, reps, or estimated 1RM, THE Progress_Tracker_Service SHALL emit a PR notification event for display in the Workout_Coach_UI.
6. WHEN a User views a historical Session, THE Progress_Tracker_Service SHALL display the comparison label "Last time you did [weight]kg for [reps] reps" for each Exercise where prior data exists.

---

### Requirement 2: CrossFit and Benchmark Tracking

**User Story:** As a CrossFitter, I want to track my benchmark workout results, so that I can measure my fitness progress against standardised tests.

#### Acceptance Criteria

1. THE Progress_Tracker_Service SHALL maintain a catalogue of Benchmark Workouts including The Girls (e.g., Fran, Grace, Helen) and Hero WODs.
2. WHEN a User completes a Session for a Benchmark Workout, THE Progress_Tracker_Service SHALL record the result in a dedicated Benchmark history for that User.
3. WHEN a User views their Benchmark history, THE Progress_Tracker_Service SHALL return all recorded results for each Benchmark Workout in chronological order.
4. THE Progress_Tracker_Service SHALL track AMRAP scores (rounds + reps), For Time scores (total time in seconds), and EMOM completion rates as distinct metric types.

---

### Requirement 3: Muscle Activation Heat Map

**User Story:** As a User, I want to see a visual muscle activation map, so that I can identify imbalances and ensure balanced training.

#### Acceptance Criteria

1. WHEN a User views the muscle activation heat map, THE Progress_Tracker_Service SHALL calculate the relative training volume per primary muscle group across all Sessions in the selected time window (default: last 30 days).
2. THE Progress_Tracker_Service SHALL return muscle activation data as a percentage of total volume per muscle group, enabling the Workout_Coach_UI to render a proportional heat map overlay.
3. THE Progress_Tracker_Service SHALL support time window selection of 7 days, 30 days, and 90 days for the heat map calculation.

---

### Requirement 4: Data Ownership and Schema Management

**User Story:** As a platform operator, I want the Progress Tracker Service to own its data exclusively, so that it can be deployed and scaled independently.

#### Acceptance Criteria

1. THE Progress_Tracker_Service SHALL consume SessionCompleted events from RabbitMQ and SHALL maintain its own read-optimised Performance data store; it SHALL NOT read from the Workout session data store directly.
2. THE Progress_Tracker_Service SHALL manage its PostgreSQL schema using Flyway versioned migration scripts.
3. WHEN the Progress_Tracker_Service starts, it SHALL apply any pending Flyway migrations before accepting traffic.
4. IF a Flyway migration fails on startup, THEN THE Progress_Tracker_Service SHALL halt startup and SHALL log the migration error with sufficient detail to identify the failing script.
