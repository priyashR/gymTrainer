# Requirements Document — Platform (Service Isolation and Integration Contracts)

## Introduction

This spec defines the cross-cutting integration contracts and data isolation rules that govern how the HybridStrength microservices interact. These rules apply to all services and must be respected during design and implementation of each service.

---

## Glossary

- **Workout_Creator_Service**: The microservice responsible for AI-powered Workout and Program creation and the Vault
- **Session_Service**: The microservice responsible for active Workout execution and Session state management
- **Progress_Tracker_Service**: The microservice responsible for performance analytics and dashboards
- **Auth_Service**: The microservice responsible for User registration, authentication, and authorisation
- **SessionCompleted**: A domain event published by the Session_Service when a User completes a workout Session

---

## Requirements

### Requirement 1: Data Integrity and Service Isolation

**User Story:** As a platform operator, I want each microservice to own its data, so that services can be deployed and scaled independently.

#### Acceptance Criteria

1. THE Workout_Creator_Service SHALL own and manage the Workout data store exclusively; no other service SHALL read from or write directly to it.
2. THE Session_Service SHALL retrieve Workout and Program definitions by calling the Workout_Creator_Service API; THE Session_Service SHALL NOT query the Workout data store directly.
3. THE Session_Service SHALL own and manage the Workout session data store exclusively; no other service SHALL read from or write directly to it.
4. THE Progress_Tracker_Service SHALL consume SessionCompleted events from RabbitMQ and SHALL maintain its own read-optimised Performance data store; it SHALL NOT read from the Workout session data store directly.
5. THE Auth_Service SHALL own and manage the User data store exclusively; all other services SHALL validate identity via JWT verification only and SHALL NOT query the User data store directly.
6. WHEN a SessionCompleted event cannot be delivered to RabbitMQ, THE Session_Service SHALL retry delivery using an exponential backoff strategy with a maximum of 5 attempts before logging the failure for manual intervention.
