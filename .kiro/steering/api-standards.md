# API Standards

## REST Conventions

- All REST APIs use JSON (`Content-Type: application/json`)
- Resource URLs use kebab-case nouns in plural form: `/workout-programs`, `/active-sessions`
- No verbs in URLs — use HTTP methods to express intent
- API versioning via URL prefix: `/api/v1/...`
- All endpoints require a valid JWT in the `Authorization: Bearer <token>` header unless explicitly public (e.g. `/api/v1/auth/register`, `/api/v1/auth/login`)

## HTTP Status Codes

| Scenario | Status Code |
|----------|-------------|
| Successful read | 200 OK |
| Successful creation | 201 Created |
| Successful update | 200 OK |
| Successful delete | 204 No Content |
| Validation failure | 400 Bad Request |
| Missing or invalid JWT | 401 Unauthorised |
| Insufficient permissions | 403 Forbidden |
| Resource not found | 404 Not Found |
| Duplicate resource | 409 Conflict |
| External service error | 502 Bad Gateway |
| Unexpected server error | 500 Internal Server Error |

## Error Response Shape

All error responses MUST use this structure:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Password must be at least 8 characters",
  "path": "/api/v1/auth/register",
  "timestamp": "2026-04-22T10:15:30Z"
}
```

- `message` must be human-readable and actionable
- Never expose stack traces, internal class names, or SQL in error responses
- Validation errors on multiple fields should use an `errors` array:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "errors": [
    { "field": "email", "message": "must be a valid email address" },
    { "field": "password", "message": "must be at least 8 characters" }
  ],
  "path": "/api/v1/auth/register",
  "timestamp": "2026-04-22T10:15:30Z"
}
```

## Pagination

All list endpoints that may return unbounded results MUST support pagination:

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 143,
  "totalPages": 8
}
```

- Default page size: 20
- Maximum page size: 100
- Query params: `?page=0&size=20&sort=createdAt,desc`
- Use Spring Data's `Pageable` and `Page<T>` — do not implement manually

## Cross-Service API Calls

- Services call each other via HTTP REST using a typed client (e.g. Spring's `RestClient` or `WebClient`)
- All inter-service calls MUST propagate the user's JWT in the `Authorization` header
- Wrap inter-service calls with a Resilience4j circuit breaker
- Timeouts: 10 seconds for Gemini API calls; 5 seconds for inter-service calls

## RabbitMQ Event Contracts

- Event payloads are JSON serialized via `Jackson2JsonMessageConverter`
- Event names use PascalCase: `SessionCompleted`, `PrAchieved`
- Each event MUST include: `eventId` (UUID), `occurredAt` (ISO-8601), `userId`, and domain-specific fields
- Consumers MUST be idempotent — duplicate delivery is possible
