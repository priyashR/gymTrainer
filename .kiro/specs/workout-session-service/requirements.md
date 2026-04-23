# Requirements Document — Workout Session Service

## Introduction

The Workout Session Service is responsible for active Workout execution, Theater Mode state management, live performance logging, and Program progression tracking. It owns the Workout session data store and publishes domain events to RabbitMQ for consumption by the Progress Tracker Service.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Workout**: A single training session definition containing one or more Sections
- **Program**: A structured collection of Workouts spanning one or more weeks
- **Section**: A named block within a Workout (e.g., a strength block, an AMRAP, a Tabata interval)
- **Exercise**: A single movement within a Section (e.g., Back Squat, Box Jump)
- **Theater Mode**: The distraction-free active workout execution UI
- **Session**: A single in-progress or completed execution of a Workout by a User
- **Performance Log**: A record of a User's results for a completed Session
- **RPE**: Rate of Perceived Exertion — a subjective effort scale (1–10)
- **AMRAP**: As Many Rounds As Possible — a timed Section type
- **EMOM**: Every Minute On the Minute — an interval Section type
- **Tabata**: A specific interval protocol (20s work / 10s rest, 8 rounds)
- **For Time**: A Section type where the User completes a fixed amount of work as fast as possible
- **CrossFit Score**: A result recorded for AMRAP (rounds + reps), EMOM, Tabata, or For Time Sections
- **Session_Service**: The microservice responsible for active Workout execution and Session state management
- **Workout_Creator_Service**: The microservice responsible for AI-powered Workout and Program creation and the Vault
- **Workout_Coach_UI**: The single frontend application serving all user-facing views

---

## Requirements

### Requirement 1: Active Workout — Theater Mode

**User Story:** As a User, I want a distraction-free workout execution interface, so that I can focus entirely on training without navigating menus.

#### Acceptance Criteria

1. WHEN a User starts a Workout, THE Workout_Coach_UI SHALL enter Theater Mode, displaying only the current Section, its Exercises, and the active timer.
2. THE Session_Service SHALL detect the Section type and configure the timer accordingly: countdown for AMRAP, stopwatch for Strength, and interval for Tabata and EMOM.
3. WHILE a Session is active, THE Workout_Coach_UI SHALL display navigation controls to move to the next or previous Section.
4. WHEN a User checks off an Exercise within a Section, THE Session_Service SHALL record the completion and SHALL automatically start the rest timer for that Exercise, using the default duration defined in the Exercise definition.
5. THE Workout_Coach_UI SHALL allow a User to adjust the rest timer duration during an active Session; the adjusted duration SHALL apply to the current rest period only and SHALL NOT modify the Exercise definition.
6. WHEN the rest timer expires, THE Workout_Coach_UI SHALL display a visual and audible notification to resume the next Exercise.
7. WHILE a Session is active, THE Workout_Coach_UI SHALL display a "Next Up" indicator showing the name of the next Exercise within the current Section and, where applicable, the name of the next Section.
8. WHILE a Session is active, THE Session_Service SHALL persist the current Session state to the Workout session data store after each Section completion, so that the Session can be resumed if the application closes.
9. WHEN a User reopens the application with an incomplete Session, THE Workout_Coach_UI SHALL offer a "Resume Session" action that restores the Session to the last persisted state.

---

### Requirement 2: Live Performance Logging

**User Story:** As a User, I want to log my performance during a workout, so that my results are captured accurately for progress tracking.

#### Acceptance Criteria

1. WHEN a User completes a Strength Exercise set, THE Session_Service SHALL accept and persist the weight lifted, number of repetitions, and RPE value for that set.
2. WHEN a User completes an AMRAP, EMOM, or For Time Section, THE Session_Service SHALL accept and persist the CrossFit Score (rounds completed, additional reps, and total time where applicable).
3. DURING an active AMRAP Section, THE Workout_Coach_UI SHALL display a tap-based round counter that increments the round count with each tap, and SHALL provide a field for the User to log additional reps at the end of the Section.
4. WHEN a User ends a Session, THE Session_Service SHALL mark the Session as complete and SHALL publish a SessionCompleted event to the message broker for consumption by the Progress_Tracker_Service.
5. IF a User attempts to end a Session with no logged data, THEN THE Session_Service SHALL prompt the User to confirm before marking the Session as complete.

---

### Requirement 3: Session State and Program Progression

**User Story:** As a User, I want the app to track where I am in my program, so that I always know what to do next without manual tracking.

#### Acceptance Criteria

1. WHEN a User completes a Session that is part of a Program, THE Session_Service SHALL advance the Program's current day pointer to the next scheduled Workout.
2. WHEN a User views the home screen, THE Workout_Coach_UI SHALL display the next scheduled Program Workout as a "Next Step" indicator, showing the Program name, current week number, day number, and Workout name alongside a "Start" action.
3. WHEN a User completes the final Session of a Program, THE Session_Service SHALL mark the Program as complete and SHALL notify the User via the UI.
4. THE Session_Service SHALL allow a User to start a new standalone Workout or a new Program independently of any active Program.
5. WHILE a Program is active, THE Session_Service SHALL allow a User to continue the current Program from the home screen with a single action.
6. WHEN a User navigates away from a Workout Details screen or cancels before starting a Session, THE Session_Service SHALL NOT advance or terminate the User's active Program state.

---

### Requirement 4: Service Integration and Data Ownership

**User Story:** As a platform operator, I want the Session Service to own its data and integrate cleanly with other services, so that it can be deployed and scaled independently.

#### Acceptance Criteria

1. THE Session_Service SHALL retrieve Workout and Program definitions by calling the Workout_Creator_Service API; THE Session_Service SHALL NOT query the Workout data store directly.
2. THE Session_Service SHALL own and manage the Workout session data store exclusively; no other service SHALL read from or write directly to it.
3. WHEN a SessionCompleted event cannot be delivered to RabbitMQ, THE Session_Service SHALL retry delivery using an exponential backoff strategy with a maximum of 5 attempts before logging the failure for manual intervention.
4. THE Session_Service SHALL manage its PostgreSQL schema using Flyway versioned migration scripts.
5. WHEN the Session_Service starts, it SHALL apply any pending Flyway migrations before accepting traffic.
6. IF a Flyway migration fails on startup, THEN THE Session_Service SHALL halt startup and SHALL log the migration error with sufficient detail to identify the failing script.
