# Tech Stack

## Backend

| Concern | Technology |
|---------|------------|
| Framework | Spring Boot 3.x (Java 17) |
| Security | Spring Security + JWT (RS256 access tokens, HttpOnly refresh cookies) |
| Batch Processing | Spring Batch (email polling, archival, scheduled jobs) |
| Database | PostgreSQL 16 (prod), H2 (dev/test) |
| Schema Management | Flyway (version-controlled migrations) |
| Messaging | RabbitMQ 3.13+ (AMQP 0-9-1) via Spring AMQP |
| Real-time | Spring WebSocket + STOMP protocol |
| Build Tool | Maven with Maven Wrapper (mvnw / mvnw.cmd) |
| Containerization | Rancher Desktop (local dev) |

## Frontend

| Concern | Technology |
|---------|------------|
| Framework | React 18 (SPA) |
| Build Tool | Vite |
| Routing | React Router v6 (client-side) |
| Language | TypeScript |
| WebSocket Client | @stomp/stompjs |

## Testing

| Layer | Technology |
|-------|------------|
| Unit Tests | JUnit 5 + Mockito |
| Property-Based Tests | jqwik (minimum 100 iterations per property) |
| Integration Tests | @SpringBootTest connecting to the local dev instances of PostgreSQL and RabbitMQ (see `docker/` for the dev stack) — ~~Testcontainers~~ not used due to local environment constraints |
| Frontend Unit | Vitest + React Testing Library |
| E2E | Playwright |

## Key Libraries

- **Jackson** — JSON serialization; use `Jackson2JsonMessageConverter` for RabbitMQ messages
- **Lombok** — boilerplate reduction (use judiciously; avoid on domain objects)
- **Jakarta Bean Validation** — input validation via annotations (`@NotNull`, `@Size`, etc.)
- **Logstash Logback Encoder** — structured JSON logging for all services
- **Spring Boot Actuator** — health checks and metrics endpoints
- **Micrometer** — custom business metrics
- **Resilience4j** — circuit breaker for external dependencies (e.g. Gemini API)
- **HikariCP** — connection pooling (auto-configured by Spring Boot; tune pool size per service load)
