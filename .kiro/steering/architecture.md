# Architecture

## Style

Hexagonal architecture (ports and adapters) applied within each microservice. Microservice architecture across domains.

## Services

| Service | Responsibility |
|---------|---------------|
| `auth-service` | User registration, authentication, JWT issuance, admin user management |
| `workout-creator-service` | AI workout/program generation via Gemini, Vault CRUD, search |
| `workout-session-service` | Theater Mode, live logging, session state, program progression |
| `progress-tracker-service` | Analytics, dashboards, benchmarks, heat map |
| `workout-coach-ui` | Next.js SSR frontend, all user-facing views |

## Hexagonal Architecture Rules

Each service must follow this internal structure:

```
domain/          # Pure domain objects and business logic — no framework dependencies
ports/
  inbound/       # Interfaces the application exposes (use cases / service interfaces)
  outbound/      # Interfaces the application depends on (repositories, external APIs)
adapters/
  inbound/       # Controllers, message listeners, WebSocket handlers
  outbound/      # Repository implementations, Gemini client, RabbitMQ publishers
application/     # Use case implementations — orchestrates domain and ports
```

- Domain objects MUST NOT import Spring, JPA, or any framework class
- Ports are interfaces only — no implementation logic
- Adapters implement ports — they are the only place framework code lives
- Use cases depend on inbound ports and call outbound ports — never call adapters directly

## Service Isolation Rules

- Each service owns its database schema exclusively — no other service reads from or writes to it
- Cross-service data access MUST go through the owning service's REST API
- The Session Service retrieves Workout definitions by calling the Workout Creator Service API
- The Progress Tracker Service receives data exclusively via RabbitMQ `SessionCompleted` events
- All services validate identity by verifying JWTs — no service calls the Auth Service DB directly

## Communication Patterns

- Synchronous: REST (JSON over HTTP) for request/response interactions
- Asynchronous: RabbitMQ (AMQP) for domain events between services
- Real-time: Spring WebSocket + STOMP for live session updates to the UI

## Database

- Each service has its own PostgreSQL schema (or separate database in production)
- H2 in-memory database for dev/test only
- Flyway manages all schema migrations — no manual DDL changes
- Migrations run automatically on service startup before traffic is accepted

## Frontend Architecture

- Next.js App Router with SSR-first rendering
- Pages are server-rendered by default
- Client components (`"use client"`) used only where interactivity is required (e.g. Theater Mode timers, AMRAP lap counter)
- No global client-side routing as the primary navigation pattern
