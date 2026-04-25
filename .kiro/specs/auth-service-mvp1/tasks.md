# Implementation Plan: Auth Service MVP1

## Overview

Incremental implementation of the Auth Service MVP1 following hexagonal architecture. Tasks are ordered by dependency: project scaffold → domain entities → outbound ports/adapters → application services → inbound adapters (controllers) → security filter → exception handling → wiring and integration tests. Each task builds on the previous and ends with everything connected.

## Tasks

- [x] 1. Scaffold Spring Boot project and configure dependencies
  - Initialize `auth-service` Maven project with Spring Boot 3.x parent, Java 17
  - Add dependencies: spring-boot-starter-web, spring-boot-starter-security, spring-boot-starter-data-jpa, spring-boot-starter-validation, flyway-core, postgresql, h2 (test), jjwt (or nimbus-jose-jwt for RS256), jqwik, testcontainers, lombok
  - Create `AuthServiceApplication.java` in `com.gmail.ramawthar.priyash.hybridstrength.authservice`
  - Create `application.yml`, `application-dev.yml`, `application-prod.yml`, `application-test.yml`
  - Create `logback-spring.xml` with Logstash JSON encoder
  - _Requirements: 2.1_

- [x] 2. Create Flyway migration scripts
  - [x] 2.1 Create `V001__create_users_table.sql` with users table, unique email index
    - Schema as defined in design: id (UUID PK), email, password_hash, role, created_at, updated_at
    - _Requirements: 2.2, 2.3_
  - [x] 2.2 Create `V002__create_refresh_tokens_table.sql` with refresh_tokens table, FK to users, indexes
    - Schema as defined in design: id (UUID PK), token_hash, user_id (FK), expires_at, created_at
    - _Requirements: 2.2, 2.3_

- [x] 3. Implement domain entities and outbound ports
  - [x] 3.1 Create `User` domain entity in `registration/domain/`
    - Pure Java, no framework imports. Fields: id, email, passwordHash, role, createdAt, updatedAt
    - _Requirements: 1.1, 2.1_
  - [x] 3.2 Create `RefreshToken` domain entity in `authentication/domain/`
    - Pure Java. Fields: id, tokenHash, userId, expiresAt, createdAt
    - _Requirements: 1.4_
  - [x] 3.3 Create `TokenPair` value object in `authentication/domain/`
    - Holds accessToken and refreshToken strings
    - _Requirements: 1.3_
  - [x] 3.4 Create outbound port interfaces
    - `UserRepository` in `registration/ports/outbound/` (findByEmail, save, existsByEmail)
    - `RefreshTokenRepository` in `authentication/ports/outbound/` (save, findByToken, deleteByUserId)
    - `PasswordEncoder` in `authentication/ports/outbound/` (encode, matches)
    - `TokenProvider` in `authentication/ports/outbound/` (generateAccessToken, generateRefreshToken, extractUserId, validateAccessToken)
    - _Requirements: 1.1, 1.3, 1.4, 1.6_
  - [x] 3.5 Create inbound port interfaces
    - `RegisterUserUseCase` in `registration/ports/inbound/`
    - `LoginUseCase` in `authentication/ports/inbound/`
    - `RefreshTokenUseCase` in `authentication/ports/inbound/`
    - _Requirements: 1.1, 1.3, 1.4_

- [x] 4. Implement outbound adapters
  - [x] 4.1 Implement `JpaUserRepository` in `registration/adapters/outbound/`
    - JPA entity mapping for User, implements UserRepository port
    - _Requirements: 1.1, 2.1_
  - [x] 4.2 Implement `JpaRefreshTokenRepository` in `authentication/adapters/outbound/`
    - JPA entity mapping for RefreshToken, implements RefreshTokenRepository port
    - _Requirements: 1.4_
  - [x] 4.3 Implement `BcryptPasswordEncoder` in `authentication/adapters/outbound/`
    - Wraps Spring Security's BCryptPasswordEncoder with cost factor 12
    - Implements PasswordEncoder outbound port
    - _Requirements: 1.6_
  - [x] 4.4 Implement `JwtTokenProvider` in `authentication/adapters/outbound/`
    - RS256 signing with RSA key pair, implements TokenProvider port
    - Access token: 15 min expiry, claims: sub (userId), email, role
    - Refresh token: opaque UUID/secure random string
    - _Requirements: 1.3, 1.4, 1.5_
  - [x] 4.5 Create `JwtConfig` in `config/`
    - Load RSA key pair from configuration, expose token expiry settings
    - _Requirements: 1.3, 1.5_

- [x] 5. Checkpoint — Verify project compiles and migrations run
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement registration use case and controller
  - [x] 6.1 Implement `RegisterUserService` in `registration/application/`
    - Implements RegisterUserUseCase: check duplicate email, encode password, persist user
    - Throws DuplicateEmailException on duplicate email
    - _Requirements: 1.1, 1.2, 1.6_
  - [x] 6.2 Write unit tests for `RegisterUserService`
    - Test successful registration, duplicate email rejection, password encoding delegation
    - Mock UserRepository and PasswordEncoder
    - Naming: `MethodName_StateUnderTest_ExpectedBehaviour`
    - _Requirements: 1.1, 1.2, 1.6_
  - [x] 6.3 Write property test: Registration round-trip (Property 1)
    - **Property 1: Registration round-trip**
    - For any valid email and password (≥ 8 chars), registering then looking up by email returns user with same email and non-null bcrypt hash ≠ raw password
    - Use in-memory fakes for outbound ports
    - `@Property(tries = 100)` in `RegistrationPropertyTest`
    - **Validates: Requirements 1.1**
  - [x] 6.4 Write property test: Duplicate email rejection (Property 2)
    - **Property 2: Duplicate email rejection**
    - For any email, if user already exists, second registration throws DuplicateEmailException and repository still contains exactly one user with that email
    - `@Property(tries = 100)` in `RegistrationPropertyTest`
    - **Validates: Requirements 1.2**
  - [x] 6.5 Create request/response DTOs for registration
    - `RegisterRequest` record with `@NotNull @Email email`, `@NotNull @Size(min = 8) password`
    - `RegisterResponse` record with id, email, createdAt
    - _Requirements: 1.1_
  - [x] 6.6 Implement `RegistrationController` in `registration/adapters/inbound/`
    - POST `/api/v1/auth/register` — public endpoint, returns 201 Created
    - Validates request body with Jakarta Bean Validation
    - _Requirements: 1.1, 1.2_

- [x] 7. Implement authentication use cases and controller
  - [x] 7.1 Implement `LoginService` in `authentication/application/`
    - Implements LoginUseCase: find user by email, verify password, generate token pair, store refresh token (hashed)
    - Throws InvalidCredentialsException on wrong email or password
    - _Requirements: 1.3, 1.6_
  - [x] 7.2 Implement `RefreshTokenService` in `authentication/application/`
    - Implements RefreshTokenUseCase: validate refresh token, check expiry, issue new access token, rotate refresh token
    - Throws InvalidRefreshTokenException on invalid/expired token
    - _Requirements: 1.4_
  - [x] 7.3 Write unit tests for `LoginService`
    - Test successful login, wrong password rejection, unknown email rejection
    - Mock all outbound ports
    - _Requirements: 1.3_
  - [ ]* 7.4 Write unit tests for `RefreshTokenService`
    - Test successful refresh, expired token rejection, invalid token rejection, token rotation
    - Mock RefreshTokenRepository and TokenProvider
    - _Requirements: 1.4_
  - [ ]* 7.5 Write property test: Login produces valid JWT (Property 3)
    - **Property 3: Login produces valid JWT**
    - For any registered user with correct credentials, login returns non-null access token that is parseable, contains correct sub/email claims, and has future expiry
    - `@Property(tries = 100)` in `AuthenticationPropertyTest`
    - **Validates: Requirements 1.3**
  - [ ]* 7.6 Write property test: Token refresh produces new access token (Property 4)
    - **Property 4: Token refresh produces new access token**
    - For any user with valid refresh token, refresh returns new valid JWT with correct sub claim, and old refresh token is invalidated
    - `@Property(tries = 100)` in `AuthenticationPropertyTest`
    - **Validates: Requirements 1.4**
  - [x] 7.7 Create `LoginRequest` and `AccessTokenResponse` DTOs
    - `LoginRequest` record with `@NotNull @Email email`, `@NotNull password`
    - `AccessTokenResponse` record with accessToken, tokenType, expiresIn
    - _Requirements: 1.3_
  - [x] 7.8 Implement `AuthenticationController` in `authentication/adapters/inbound/`
    - POST `/api/v1/auth/login` — public, returns 200 with AccessTokenResponse + Set-Cookie refresh token (HttpOnly, Secure, SameSite=Strict)
    - POST `/api/v1/auth/refresh` — reads refresh token from HttpOnly cookie, returns new AccessTokenResponse + rotated cookie
    - _Requirements: 1.3, 1.4_

- [ ] 8. Checkpoint — Verify registration and authentication flows compile
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Implement security layer
  - [ ] 9.1 Implement `JwtAuthenticationFilter` in `common/security/`
    - Extends OncePerRequestFilter: extract Bearer token, validate via TokenProvider, set SecurityContext
    - Does not throw on invalid token — lets Spring Security return 401
    - _Requirements: 1.5_
  - [ ] 9.2 Implement `SecurityConfig` in `config/`
    - SecurityFilterChain bean: STATELESS sessions, CSRF disabled, public endpoints (/register, /login, /refresh), all others authenticated
    - Register JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
    - Configure AuthenticationEntryPoint to return standard ErrorResponse JSON for 401
    - CORS explicitly configured
    - _Requirements: 1.5_
  - [ ]* 9.3 Write unit tests for `JwtTokenProvider`
    - Test token generation, claim extraction, validation of valid/invalid/expired tokens
    - _Requirements: 1.3, 1.5_
  - [ ]* 9.4 Write property test: Invalid tokens are rejected (Property 5)
    - **Property 5: Invalid tokens are rejected**
    - For any string that is not a validly-signed, non-expired JWT (random strings, empty strings, expired tokens, wrong-key tokens), validateAccessToken returns false
    - `@Property(tries = 100)` in `JwtValidationPropertyTest`
    - **Validates: Requirements 1.5**

- [ ] 10. Implement exception handling
  - [ ] 10.1 Create custom exceptions in `common/exception/`
    - `DuplicateEmailException`, `InvalidCredentialsException`, `InvalidRefreshTokenException`
    - _Requirements: 1.2, 1.3, 1.4, 1.5_
  - [ ] 10.2 Create shared DTOs in `common/dto/`
    - `ErrorResponse` and `ValidationErrorResponse` records matching api-standards.md shape
    - _Requirements: 1.2, 1.5_
  - [ ] 10.3 Implement `GlobalExceptionHandler` in `common/exception/`
    - `@RestControllerAdvice`: map DuplicateEmailException → 409, InvalidCredentialsException → 401, InvalidRefreshTokenException → 401, MethodArgumentNotValidException → 400 with field errors, generic Exception → 500
    - Never expose stack traces or internal details
    - _Requirements: 1.2, 1.5_

- [ ] 11. Implement password encoding property test
  - [ ]* 11.1 Write property test: Password hashing round-trip (Property 6)
    - **Property 6: Password hashing round-trip with cost factor invariant**
    - For any raw password, encode produces hash where matches(raw, hash) is true, hash ≠ raw, and bcrypt prefix indicates cost factor ≥ 12
    - `@Property(tries = 100)` in `PasswordEncodingPropertyTest`
    - **Validates: Requirements 1.6**

- [ ] 12. Integration tests
  - [ ]* 12.1 Write integration test suite `AuthIntegrationTest`
    - `@SpringBootTest(webEnvironment = RANDOM_PORT)` with Testcontainers PostgreSQL 16
    - Test scenarios: register → login → access protected endpoint → refresh → access again; duplicate email → 409; wrong password → 401; no token → 401; expired token → 401
    - Verify Flyway migrations run on startup
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.2, 2.3_

- [ ] 13. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests use in-memory fakes (not mocks) for outbound ports per design spec
- Checkpoints ensure incremental validation at natural break points
- All code uses Java 21, Spring Boot 3.x, hexagonal architecture as defined in steering docs
