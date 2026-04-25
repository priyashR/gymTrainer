# Project Structure

## Top-Level Layout

```
hybrid-strength/
├── auth-service/               # Spring Boot — authentication and user management
├── workout-creator-service/    # Spring Boot — AI generation, Vault CRUD, search
├── workout-session-service/    # Spring Boot — Theater Mode, logging, program progression
├── progress-tracker-service/   # Spring Boot — analytics, benchmarks, heat map
├── workout-coach-ui/           # React 18 SPA (Vite + React Router) — all user-facing views
├── docker/                     # Docker Compose and init scripts for local dev
├── docs/                       # Additional documentation
└── .kiro/
    ├── specs/                  # 6 sub-specs + master spec
    └── steering/               # Steering documents (this file)
```

---

## Backend Package Structure

Package-by-feature organisation under the service root package, including "com.gmail.ramawthar.priyash". Each service follows the same hexagonal architecture layout.

### Example: `workout-creator-service`

```
src/main/java/com/gmail/ramawthar/priyash/hybridstrength/workoutcreator/
├── WorkoutCreatorApplication.java
├── config/                        # Spring configuration classes
│   ├── SecurityConfig.java
│   ├── GeminiConfig.java
│   ├── RabbitMQConfig.java
│   └── FlywayConfig.java
├── generation/                    # AI workout/program generation via Gemini
│   ├── domain/
│   ├── ports/
│   │   ├── inbound/
│   │   └── outbound/
│   ├── application/
│   └── adapters/
│       ├── inbound/               # REST controllers
│       └── outbound/              # Gemini API client
├── vault/                         # Workout and Program CRUD
│   ├── domain/
│   ├── ports/
│   ├── application/
│   └── adapters/
│       ├── inbound/               # REST controllers
│       └── outbound/              # JPA repositories
├── search/                        # Vault search and filter
│   ├── domain/
│   ├── ports/
│   ├── application/
│   └── adapters/
└── common/                        # Shared across features within this service
    ├── model/                     # Domain entities and enumerations
    ├── dto/                       # Shared DTOs and API response wrappers
    ├── exception/                 # Exception hierarchy and global handler
    └── event/                     # Domain event definitions
```

The same structure applies to each backend service, with feature packages matching the service's domain:

| Service | Feature Packages |
|---------|-----------------|
| `auth-service` | `registration`, `authentication`, `admin` |
| `workout-creator-service` | `generation`, `vault`, `search` |
| `workout-session-service` | `theater`, `logging`, `progression` |
| `progress-tracker-service` | `dashboard`, `benchmark`, `heatmap` |

---

## Backend Test Structure

```
src/test/java/com/gmail/ramawthar/priyash/hybridstrength/<service>/
├── unit/              # Unit tests (JUnit 5 + Mockito) — domain and use cases
├── property/          # Property-based tests (jqwik)
└── integration/       # Integration tests (Testcontainers + @SpringBootTest)
```

---

## Backend Resources

```
src/main/resources/
├── application.yml              # Shared defaults
├── application-dev.yml          # Local dev (H2 or Docker PostgreSQL)
├── application-prod.yml         # Production (env var placeholders)
├── logback-spring.xml           # Structured JSON logging (Logstash encoder)
└── db/migration/                # Flyway migrations
    ├── V001__create_<entity>.sql
    ├── V002__create_<entity>.sql
    └── ...

src/test/resources/
└── application-test.yml         # Test profile (H2 in-memory)
```

**Flyway migration numbering** uses reserved ranges per service:

| Range | Service |
|-------|---------|
| V001–V099 | `auth-service` |
| V100–V199 | `workout-creator-service` |
| V200–V299 | `workout-session-service` |
| V300–V399 | `progress-tracker-service` |

---

## Frontend Structure

```
workout-coach-ui/
├── index.html                  # Vite entry point
├── src/
│   ├── main.tsx                # App bootstrap and router setup
│   ├── App.tsx                 # Root component with route definitions
│   ├── pages/                  # Route-level page components
│   │   ├── Home.tsx
│   │   ├── workout/
│   │   │   ├── NewWorkout.tsx
│   │   │   ├── WorkoutDetail.tsx
│   │   │   └── WorkoutSession.tsx   # Theater Mode
│   │   ├── performance/
│   │   │   └── Dashboard.tsx
│   │   └── auth/
│   │       ├── Login.tsx
│   │       └── Register.tsx
│   ├── components/             # Shared UI components
│   │   ├── ui/                 # Primitives (buttons, inputs, cards)
│   │   └── layout/             # Navigation, headers, wrappers
│   ├── features/               # Feature-based client modules
│   │   ├── theater/            # Theater Mode timer, lap counter, rest timer
│   │   ├── vault/              # Vault search and filter
│   │   ├── progress/           # Dashboard charts and heat map
│   │   └── auth/               # Login and registration forms
│   ├── hooks/                  # Custom React hooks
│   ├── lib/                    # API client layer and utilities
│   └── types/                  # TypeScript type definitions
├── public/                     # Static assets
├── vite.config.ts
├── tsconfig.json
└── package.json
```

---

## Spec Organisation

6 sub-specs under `.kiro/specs/`, ordered by dependency:

| Spec | Domain |
|------|--------|
| `platform` | Inter-service contracts and data isolation rules |
| `auth-service` | Registration, authentication, admin user management |
| `workout-creator-service` | AI generation, Vault CRUD, search and filter |
| `workout-session-service` | Theater Mode, live logging, program progression |
| `progress-tracker-service` | Dashboard, benchmarks, muscle heat map |
| `workout-coach-ui` | Home screen, navigation, SPA frontend |

Master spec: `hybrid-strength-app/` — full platform requirements, source of truth before split.

---

## Key Conventions

- **Package-by-feature**, not package-by-layer. Related domain, ports, application, and adapter code stays together within each feature.
- **`common/`** within each service holds shared entities, DTOs, exceptions, and events that cross feature boundaries within that service.
- **No cross-service DB access** — services communicate via REST API or RabbitMQ events only.
- **Hexagonal layers** within each feature: `domain` → `ports` → `application` → `adapters`. Domain has zero framework imports.
- **Flyway migration numbering** uses reserved ranges per service (see table above) to avoid conflicts when services share a migration history in dev.
- **Client components** — all React components are client-rendered in the SPA. No server/client component distinction.
