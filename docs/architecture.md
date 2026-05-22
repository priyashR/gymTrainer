# HybridStrength — Architecture Diagrams

## System Architecture

```mermaid
graph TB
    subgraph "Client"
        UI[Workout Coach UI<br/>React SPA - Vite<br/>localhost:5173]
    end

    subgraph "Backend Services (Docker Compose)"
        AUTH[Auth Service<br/>Spring Boot<br/>:8081]
        CREATOR[Workout Creator Service<br/>Spring Boot<br/>:8082]
        SESSION[Workout Session Service<br/>Spring Boot<br/>:8083]
    end

    subgraph "Infrastructure (Kubernetes)"
        PG[(PostgreSQL 16<br/>hybridstrength_dev<br/>NodePort :30432)]
        RMQ[RabbitMQ 3.13<br/>NodePort :30672]
    end

    UI -->|REST /api/v1/auth/*| AUTH
    UI -->|REST /api/v1/vault/*<br/>REST /api/v1/uploads/*| CREATOR
    UI -->|REST /api/v1/sessions/*<br/>REST /api/v1/enrollments/*<br/>WebSocket /ws/sessions| SESSION

    AUTH --> PG
    CREATOR --> PG
    SESSION --> PG
    SESSION --> RMQ
    SESSION -->|REST: fetch program| CREATOR

    RMQ -.->|SessionCompleted event| PROGRESS[Progress Tracker<br/>Future]
```

## Service Responsibilities

```mermaid
graph LR
    subgraph "Auth Service :8081"
        A1[Registration]
        A2[Login / JWT Issuance]
        A3[Token Refresh]
    end

    subgraph "Workout Creator Service :8082"
        B1[Program Upload]
        B2[Vault CRUD]
        B3[Search & Filter]
    end

    subgraph "Workout Session Service :8083"
        C1[Session Lifecycle]
        C2[Program Enrollment]
        C3[Day Progression]
        C4[WebSocket Updates]
        C5[Event Publishing]
    end
```

## Database Schema (Shared: hybridstrength_dev)

```mermaid
erDiagram
    users {
        UUID id PK
        VARCHAR email UK
        VARCHAR password_hash
        TIMESTAMP created_at
    }

    refresh_tokens {
        UUID id PK
        UUID user_id FK
        VARCHAR token
        TIMESTAMP expires_at
    }

    programs {
        UUID id PK
        VARCHAR name
        INT duration_weeks
        VARCHAR goal
        VARCHAR owner_user_id
        TIMESTAMP created_at
    }

    weeks {
        UUID id PK
        UUID program_id FK
        INT week_number
    }

    days {
        UUID id PK
        UUID week_id FK
        INT day_number
        VARCHAR day_label
        VARCHAR focus_area
    }

    sections {
        UUID id PK
        UUID day_id FK
        VARCHAR name
        VARCHAR section_type
        INT sort_order
    }

    exercises {
        UUID id PK
        UUID section_id FK
        VARCHAR exercise_name
        INT prescribed_sets
        VARCHAR prescribed_reps
        INT rest_interval_seconds
    }

    sessions {
        UUID id PK
        VARCHAR user_id
        UUID program_id
        UUID enrollment_id
        VARCHAR status
        INT current_section_index
        JSONB workout_snapshot
        JSONB section_progresses
        TIMESTAMP started_at
        TIMESTAMP completed_at
    }

    program_enrollments {
        UUID id PK
        VARCHAR user_id
        UUID program_id
        VARCHAR program_name
        INT current_week
        INT current_day
        VARCHAR status
        TIMESTAMP enrolled_at
    }

    skip_records {
        UUID id PK
        UUID enrollment_id FK
        INT week_number
        INT day_number
        TIMESTAMP skipped_at
    }

    users ||--o{ refresh_tokens : has
    programs ||--o{ weeks : contains
    weeks ||--o{ days : contains
    days ||--o{ sections : contains
    sections ||--o{ exercises : contains
    program_enrollments ||--o{ skip_records : has
```

## Hexagonal Architecture (Per Service)

```mermaid
graph TB
    subgraph "Inbound Adapters"
        REST[REST Controllers]
        WS[WebSocket Handler]
    end

    subgraph "Application Layer"
        UC[Use Cases / Services]
    end

    subgraph "Domain"
        DOM[Domain Objects<br/>Business Logic<br/>No framework imports]
    end

    subgraph "Outbound Adapters"
        JPA[JPA Repositories]
        RABBIT[RabbitMQ Publisher]
        HTTP[REST Client<br/>WorkoutFetcher]
    end

    subgraph "Ports"
        IP[Inbound Ports<br/>Use Case Interfaces]
        OP[Outbound Ports<br/>Repository Interfaces]
    end

    REST --> IP
    WS --> IP
    IP --> UC
    UC --> DOM
    UC --> OP
    OP --> JPA
    OP --> RABBIT
    OP --> HTTP
```

## Authentication Flow

```mermaid
sequenceDiagram
    participant UI as React SPA
    participant AUTH as Auth Service
    participant SVC as Other Services

    UI->>AUTH: POST /api/v1/auth/login
    AUTH-->>UI: accessToken (body) + refreshToken (HttpOnly cookie)

    UI->>SVC: GET /api/v1/vault/programs<br/>Authorization: Bearer {accessToken}
    SVC->>SVC: Verify JWT signature (RS256 public key)
    SVC-->>UI: 200 OK

    Note over UI: Token expires (15 min)

    UI->>SVC: GET /api/v1/sessions/active
    SVC-->>UI: 401 Unauthorized

    UI->>AUTH: POST /api/v1/auth/refresh<br/>(cookie sent automatically)
    AUTH-->>UI: new accessToken

    UI->>SVC: Retry original request with new token
    SVC-->>UI: 200 OK
```

## Inter-Service Communication

```mermaid
graph LR
    subgraph "Synchronous (REST)"
        SESSION -->|GET /api/v1/vault/programs/:id<br/>JWT forwarded| CREATOR
    end

    subgraph "Asynchronous (RabbitMQ)"
        SESSION -->|SessionCompleted event<br/>Exchange: session.events<br/>Key: session.completed| QUEUE[Queue]
        QUEUE -->|Future| PROGRESS[Progress Tracker]
    end
```

## Deployment Topology (Local Dev)

```mermaid
graph TB
    subgraph "Host Machine"
        VITE[Vite Dev Server :5173]
        subgraph "Docker Compose"
            AUTH_C[auth-service :8081]
            CREATOR_C[workout-creator-service :8082]
            SESSION_C[workout-session-service :8083]
        end
    end

    subgraph "Rancher Desktop (k8s)"
        PG_K[PostgreSQL Pod<br/>NodePort :30432]
        RMQ_K[RabbitMQ Pod<br/>NodePort :30672]
    end

    VITE -->|Proxy| AUTH_C
    VITE -->|Proxy| CREATOR_C
    VITE -->|Proxy| SESSION_C

    AUTH_C -->|host.docker.internal:30432| PG_K
    CREATOR_C -->|host.docker.internal:30432| PG_K
    SESSION_C -->|host.docker.internal:30432| PG_K
    SESSION_C -->|host.docker.internal:30672| RMQ_K
    SESSION_C -->|http://workout-creator-service:8082| CREATOR_C
```
