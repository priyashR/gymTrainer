# Requirements Document — Auth Service

## Introduction

The Auth Service is responsible for User registration, authentication, authorisation, and admin user management for the HybridStrength platform. It is a standalone microservice following hexagonal architecture principles. All other services validate identity by verifying JWTs issued by this service.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Admin**: A privileged User with access to administrative functions
- **Auth_Service**: The microservice responsible for User registration, authentication, and authorisation
- **JWT**: JSON Web Token — a signed token used to authenticate requests to protected endpoints

---

## Requirements

### Requirement 1: User Registration and Authentication

**User Story:** As a visitor, I want to register and log in, so that I can access my personal training data securely.

#### Acceptance Criteria

1. THE Auth_Service SHALL provide a registration endpoint that accepts a unique email address and a password of at least 8 characters, and SHALL persist the credentials to the User data store.
2. WHEN a registration request is received with a duplicate email address, THE Auth_Service SHALL return a 409 Conflict response with a descriptive error message.
3. WHEN a valid login request is received, THE Auth_Service SHALL return a signed JWT access token and a refresh token.
4. WHEN an expired JWT is presented with a valid refresh token, THE Auth_Service SHALL issue a new JWT access token without requiring re-authentication.
5. WHEN an invalid or missing JWT is presented to any protected endpoint, THE Auth_Service SHALL return a 401 Unauthorised response.
6. THE Auth_Service SHALL store passwords using a one-way cryptographic hash (bcrypt with a minimum cost factor of 12).

---

### Requirement 2: Admin User Management

> **Out of scope for MVP.** To be implemented in a future iteration.

**User Story:** As an Admin, I want to manage User accounts, so that I can maintain platform integrity.

#### Acceptance Criteria

1. THE Auth_Service SHALL expose admin-only endpoints protected by a role-based access control check for the ADMIN role.
2. WHEN an Admin requests a list of Users, THE Auth_Service SHALL return a paginated list of User accounts including email, registration date, and account status.
3. WHEN an Admin deactivates a User account, THE Auth_Service SHALL prevent that User from authenticating until the account is reactivated.
4. IF a non-Admin User attempts to access an admin-only endpoint, THEN THE Auth_Service SHALL return a 403 Forbidden response.

---

### Requirement 3: Data Ownership and Schema Management

**User Story:** As a platform operator, I want the Auth Service to own its data exclusively, so that it can be deployed and scaled independently.

#### Acceptance Criteria

1. THE Auth_Service SHALL own and manage the User data store exclusively; all other services SHALL validate identity via JWT verification only and SHALL NOT query the User data store directly.
2. THE Auth_Service SHALL manage its PostgreSQL schema using Flyway versioned migration scripts.
3. WHEN the Auth_Service starts, it SHALL apply any pending Flyway migrations before accepting traffic.
4. IF a Flyway migration fails on startup, THEN THE Auth_Service SHALL halt startup and SHALL log the migration error with sufficient detail to identify the failing script.
