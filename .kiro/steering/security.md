# Security Standards

## Authentication

- JWT-based authentication using RS256 (asymmetric signing)
- Access tokens: short-lived (15 minutes recommended), delivered in the response body
- Refresh tokens: long-lived (7 days recommended), stored in HttpOnly, Secure, SameSite=Strict cookies
- All protected endpoints require a valid `Authorization: Bearer <token>` header
- Token verification is performed locally in each service using the public key — no Auth Service call per request

## Password Storage

- Passwords hashed with bcrypt, minimum cost factor 12
- Never log, return, or store plaintext passwords at any point
- Password reset tokens are single-use, time-limited (60 minutes), and invalidated on use

## Authorisation

- Role-based access control (RBAC) via Spring Security
- Roles: `USER` (default), `ADMIN`
- Admin-only endpoints must be protected with `@PreAuthorize("hasRole('ADMIN')")`
- Resource ownership enforced at the service layer — always verify the authenticated user's identity matches the resource owner before read, update, or delete
- Return 403 Forbidden (not 404) when a user attempts to access another user's resource — do not leak resource existence

## Input Validation

- All inbound request bodies validated with Jakarta Bean Validation annotations
- Validation errors return 400 Bad Request with field-level detail (see api-standards.md)
- Never trust client-supplied IDs for ownership checks — always resolve ownership from the JWT subject claim

## Sensitive Data

- Never log JWT tokens, passwords, refresh tokens, or PII
- Structured logs (Logstash Logback Encoder) must not include sensitive fields
- Use placeholder values in error messages — never expose internal identifiers or database keys in API responses

## Transport Security

- All traffic over HTTPS in production
- HttpOnly and Secure flags required on all cookies
- CORS policy must be explicitly configured — do not use wildcard origins in production

## External Dependencies

- Gemini API calls must use Resilience4j circuit breaker to prevent cascading failures
- API keys and secrets stored in environment variables or a secrets manager — never hardcoded or committed to source control
- Validate and sanitise all content returned from Gemini before persisting or returning to the client

## Spring Security Configuration

- Disable CSRF for stateless REST endpoints (JWT-authenticated)
- Enable CSRF for any form-based or cookie-authenticated flows
- Use `SecurityFilterChain` bean configuration — do not extend `WebSecurityConfigurerAdapter` (deprecated in Spring Security 6)
- Session management: `STATELESS` for all REST services
