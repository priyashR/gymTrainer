# Requirements Document — Auth Service (MVP2)

## Introduction

MVP2 of the Auth Service adds admin user management and password reset functionality. These features build on the MVP1 foundation and are not required for initial platform operation.

**Prerequisite:** Auth Service MVP1 must be implemented first.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Admin**: A privileged User with access to administrative functions
- **Auth_Service**: The microservice responsible for User registration, authentication, and authorisation
- **JWT**: JSON Web Token — a signed token used to authenticate requests to protected endpoints

---

## Requirements

### Requirement 1: Admin User Management

**User Story:** As an Admin, I want to manage User accounts, so that I can maintain platform integrity.

#### Acceptance Criteria

1. THE Auth_Service SHALL expose admin-only endpoints protected by a role-based access control check for the ADMIN role.
2. WHEN an Admin requests a list of Users, THE Auth_Service SHALL return a paginated list of User accounts including email, registration date, and account status.
3. WHEN an Admin deactivates a User account, THE Auth_Service SHALL prevent that User from authenticating until the account is reactivated.
4. IF a non-Admin User attempts to access an admin-only endpoint, THEN THE Auth_Service SHALL return a 403 Forbidden response.

---

### Requirement 2: Password Reset

**User Story:** As a User, I want to reset my password if I forget it, so that I can regain access to my account.

#### Acceptance Criteria

1. WHEN a User requests a password reset, THE Auth_Service SHALL send a time-limited reset link to the registered email address, valid for no more than 60 minutes.
2. THE password reset token SHALL be single-use and SHALL be invalidated after successful use or expiry.
3. WHEN a valid reset token is submitted with a new password, THE Auth_Service SHALL update the stored password hash and return a success response.
4. WHEN an invalid or expired reset token is submitted, THE Auth_Service SHALL return a 400 Bad Request response with a descriptive error message.
