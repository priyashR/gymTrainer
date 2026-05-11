# HybridStrength — Architecture, Design & Status Diagrams

---

## 1. Component Diagram

All logical components of the HybridStrength platform and their responsibilities.

```mermaid
graph TB
    subgraph "Frontend"
        UI[Workout Coach UI<br/>React 18 SPA · Vite · TypeScript]
    end

    subgraph "Backend Services"
        AUTH[Auth Service<br/>Spring Boot 3.x]
        WCS[Workout Creator Service<br/>Spring Boot 3.x]
        WSS[Workout Session Service<br/>Spring Boot 3.x]
        PTS[Progress Tracker Service<br/>Spring Boot 3.x]
    end

    subgraph "Data Stores"
        AUTH_DB[(Auth DB<br/>PostgreSQL)]
        WCS_DB[(Workout DB<br/>PostgreSQL)]
        WSS_DB[(Session DB<br/>PostgreSQL)]
        PTS_DB[(Performance DB<br/>PostgreSQL)]
    end

    subgraph "Infrastructure"
        MQ[RabbitMQ<br/>Message Broker]
        GEMINI[Google Gemini<br/>AI API]
    end

    UI -->|REST + JWT| AUTH
    UI -->|REST + JWT| WCS
    UI -->|REST + JWT| WSS
    UI -->|WebSocket + STOMP| WSS

    AUTH --> AUTH_DB
    WCS --> WCS_DB
    WSS --> WSS_DB
    PTS --> PTS_DB

    WCS -->|Prompt/Response| GEMINI
    WSS -->|REST| WCS
    WSS -->|Publish Events| MQ
    PTS -->|Consume Events| MQ
```

### Component Responsibilities

| Component | Responsibility |
|-----------|---------------|
| **Workout Coach UI** | React SPA — auth flows, workout generation, vault management, upload, theater mode, progress dashboard |
| **Auth Service** | User registration, login, JWT issuance (RS256), refresh tokens, admin user management |
| **Workout Creator Service** | AI workout generation (Gemini), program upload/parse/validate, Vault CRUD, search & filter |
| **Workout Session Service** | Theater Mode state, live performance logging, rest timers, program progression, event publishing |
| **Progress Tracker Service** | Dashboard analytics, 1RM calculations, benchmark tracking, muscle heat map, PR detection |
| **RabbitMQ** | Async event bus — `SessionCompleted` events from Session → Progress Tracker |
| **Google Gemini** | External AI for natural language → structured workout generation |

---

## 2. End-to-End Architecture Diagram

Shows the full request/data flow from user interaction through all layers.

```mermaid
flowchart LR
    subgraph "Client"
        Browser[Browser / Mobile]
    end

    subgraph "Frontend Layer"
        SPA[React SPA<br/>Vite · React Router v6]
    end

    subgraph "API Gateway Layer"
        direction TB
        JWT_FILTER[JWT Auth Filter<br/>RS256 Verification]
    end

    subgraph "Service Layer"
        direction TB
        AS[Auth Service<br/>:8081]
        WCS_SVC[Workout Creator<br/>:8082]
        WSS_SVC[Workout Session<br/>:8083]
        PTS_SVC[Progress Tracker<br/>:8084]
    end

    subgraph "Messaging Layer"
        RMQ[RabbitMQ<br/>:5672]
    end

    subgraph "External APIs"
        GEM[Google Gemini API]
    end

    subgraph "Persistence Layer"
        PG1[(auth_db)]
        PG2[(workout_creator_db)]
        PG3[(workout_session_db)]
        PG4[(progress_tracker_db)]
    end

    Browser --> SPA
    SPA -->|HTTPS REST| JWT_FILTER
    JWT_FILTER --> AS
    JWT_FILTER --> WCS_SVC
    JWT_FILTER --> WSS_SVC
    JWT_FILTER --> PTS_SVC

    SPA -.->|WebSocket STOMP| WSS_SVC

    AS --> PG1
    WCS_SVC --> PG2
    WSS_SVC --> PG3
    PTS_SVC --> PG4

    WCS_SVC -->|HTTP| GEM
    WSS_SVC -->|REST| WCS_SVC
    WSS_SVC -->|AMQP Publish| RMQ
    RMQ -->|AMQP Consume| PTS_SVC
```

### Communication Patterns

| Pattern | From → To | Protocol | Purpose |
|---------|-----------|----------|---------|
| Synchronous | UI → Services | REST/JSON over HTTPS | All CRUD operations |
| Synchronous | Session → Creator | REST/JSON | Fetch workout definitions |
| Asynchronous | Session → Progress | RabbitMQ (AMQP) | `SessionCompleted` events |
| Real-time | UI ↔ Session | WebSocket + STOMP | Live timer updates, session state |
| External | Creator → Gemini | HTTPS | AI workout generation |

### Security Flow

```mermaid
sequenceDiagram
    participant U as User
    participant UI as React SPA
    participant AS as Auth Service
    participant SVC as Any Service

    U->>UI: Login (email + password)
    UI->>AS: POST /api/v1/auth/login
    AS-->>UI: Access Token (body) + Refresh Token (HttpOnly cookie)
    UI->>UI: Store access token in memory (never localStorage)

    U->>UI: Access protected feature
    UI->>SVC: GET /api/v1/... (Authorization: Bearer <token>)
    SVC->>SVC: Verify JWT locally (RS256 public key)
    SVC-->>UI: 200 OK + data

    Note over UI,AS: On 401 (token expired)
    UI->>AS: POST /api/v1/auth/refresh (cookie)
    AS-->>UI: New Access Token + Rotated Refresh Cookie
    UI->>SVC: Retry original request with new token
```

---

## 3. Full System Design

### Hexagonal Architecture (per service)

Each microservice follows the same internal layering:

```mermaid
graph TB
    subgraph "Inbound Adapters"
        REST[REST Controllers]
        WS[WebSocket Handlers]
        MQ_IN[Message Listeners]
    end

    subgraph "Inbound Ports"
        UC[Use Case Interfaces]
    end

    subgraph "Application Layer"
        SVC[Service Implementations]
    end

    subgraph "Domain"
        DOM[Domain Objects<br/>Pure Java · No Framework]
    end

    subgraph "Outbound Ports"
        REPO[Repository Interfaces]
        EXT[External Client Interfaces]
    end

    subgraph "Outbound Adapters"
        JPA[JPA Repositories]
        HTTP[HTTP Clients]
        MQ_OUT[RabbitMQ Publishers]
    end

    REST --> UC
    WS --> UC
    MQ_IN --> UC
    UC -.-> SVC
    SVC --> DOM
    SVC --> REPO
    SVC --> EXT
    REPO -.-> JPA
    EXT -.-> HTTP
    EXT -.-> MQ_OUT
```

### Workout Creator Service — Internal Design

```mermaid
graph TB
    subgraph "Inbound Adapters"
        UploadCtrl[UploadController<br/>POST /uploads/programs]
        VaultCtrl[VaultController<br/>GET/PUT/DELETE /vault/programs]
        GenCtrl[GenerationController<br/>POST /generate]
    end

    subgraph "Application Layer"
        UploadSvc[UploadProgramService]
        VaultSvc[VaultService]
        GenSvc[GenerationService]
    end

    subgraph "Domain"
        Program[Program]
        Week[Week]
        Day[Day]
        Section[Section]
        Exercise[Exercise]
        Parser[UploadParser]
        Formatter[UploadFormatter]
        SearchCriteria[SearchCriteria]
        VaultProgram[VaultProgram]
        VaultItem[VaultItem]
    end

    subgraph "Outbound Adapters"
        JpaUpload[JpaUploadProgramRepository]
        JpaVault[JpaVaultProgramRepository]
        GeminiClient[GeminiApiClient]
        EntityMapper[ProgramEntityMapper]
    end

    subgraph "Persistence"
        SpringData[ProgramSpringDataRepository]
        DB[(PostgreSQL<br/>programs, weeks, days,<br/>sections, exercises,<br/>warm_cool_entries)]
    end

    UploadCtrl --> UploadSvc
    VaultCtrl --> VaultSvc
    GenCtrl --> GenSvc

    UploadSvc --> Parser
    UploadSvc --> JpaUpload
    VaultSvc --> JpaVault
    VaultSvc --> Parser
    GenSvc --> GeminiClient
    GenSvc --> Formatter

    JpaUpload --> EntityMapper
    JpaVault --> EntityMapper
    EntityMapper --> SpringData
    SpringData --> DB
```

### Database Schema (Workout Creator Service)

```mermaid
erDiagram
    PROGRAMS ||--o{ WEEKS : contains
    WEEKS ||--o{ DAYS : contains
    DAYS ||--o{ SECTIONS : contains
    DAYS ||--o{ WARM_COOL_ENTRIES : contains
    SECTIONS ||--o{ EXERCISES : contains

    PROGRAMS {
        uuid id PK
        varchar name
        int duration_weeks
        varchar goal
        text equipment_profile
        varchar owner_user_id
        varchar content_source
        timestamp created_at
        timestamp updated_at
    }

    WEEKS {
        uuid id PK
        uuid program_id FK
        int week_number
    }

    DAYS {
        uuid id PK
        uuid week_id FK
        int day_number
        varchar day_label
        varchar focus_area
        varchar modality
        varchar methodology_source
    }

    SECTIONS {
        uuid id PK
        uuid day_id FK
        varchar name
        varchar section_type
        varchar format
        int time_cap
        int sort_order
    }

    EXERCISES {
        uuid id PK
        uuid section_id FK
        varchar exercise_name
        varchar modality_type
        int prescribed_sets
        varchar prescribed_reps
        varchar prescribed_weight
        int rest_interval_seconds
        text notes
        int sort_order
    }

    WARM_COOL_ENTRIES {
        uuid id PK
        uuid day_id FK
        varchar entry_type
        varchar movement
        text instruction
        int sort_order
    }
```

---

## 4. Screen Flows

### Current Application Routes

```mermaid
flowchart TD
    START[App Launch] --> AUTH_CHECK{Authenticated?}

    AUTH_CHECK -->|No| LOGIN[/login<br/>Login Page]
    AUTH_CHECK -->|Yes| HOME[/<br/>Home Page]

    LOGIN -->|Register link| REGISTER[/register<br/>Registration Page]
    REGISTER -->|Success| LOGIN
    LOGIN -->|Success| HOME

    HOME --> NW[New Workout Menu]
    HOME --> PERF[/my-performance<br/>Coming Soon]
    HOME --> WK[Workout Menu]

    NW --> ASK_GEMINI[/new-workout<br/>Ask Gemini · Coming Soon]
    NW --> UPLOAD[/upload<br/>Upload Program]

    WK --> CONTINUE[/workout/continue<br/>Continue with Program · Coming Soon]
    WK --> SEARCH[/vault/search<br/>Vault Search Page]

    UPLOAD -->|Success| VAULT_LINK[View in Vault link]
    VAULT_LINK --> DETAIL

    SEARCH -->|Select result| DETAIL[/vault/programs/:id<br/>Program Detail Page]
    DETAIL -->|Delete| SEARCH
    DETAIL -->|Copy| DETAIL_COPY[/vault/programs/:newId<br/>Copy Detail Page]
    DETAIL -->|Edit JSON| EDITOR[Inline JSON Editor]
    EDITOR -->|Save| DETAIL
```

### Home Page Menu Structure

```
┌─────────────────────────────────────────┐
│              HOME SCREEN                 │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐    │
│  │  🏋️ New Workout  [expandable]   │    │
│  │    ├── Ask Gemini               │    │
│  │    └── Upload Program           │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │  📊 My Performance              │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │  💪 Workout  [expandable]       │    │
│  │    ├── Continue with Program    │    │
│  │    └── Search for a workout     │    │
│  └─────────────────────────────────┘    │
│                                         │
│  [Logout]                               │
└─────────────────────────────────────────┘
```

### Upload Flow

```
┌──────────┐    ┌──────────────┐    ┌──────────────┐    ┌─────────────┐
│  File    │───▶│  Structured  │───▶│   Uploading  │───▶│   Success   │
│  Picker  │    │   Preview    │    │  (disabled)  │    │ + Vault Link│
└──────────┘    └──────────────┘    └──────────────┘    └─────────────┘
                       │                                        
                       ▼                                        
                ┌──────────────┐                               
                │  JSON Editor │                               
                │  (optional)  │                               
                └──────────────┘                               
```

### Vault Search & Detail Flow

```
┌─────────────────────────────────────────────────────────┐
│  VAULT SEARCH PAGE  (/vault/search)                     │
├─────────────────────────────────────────────────────────┤
│  [Search input ___________] [🔍]                        │
│  Focus Area: [All ▼]   Modality: [All ▼]               │
│                                                         │
│  ┌─────────────────────────────────────────────┐        │
│  │ Program Name          │ Goal │ Weeks │ Source│        │
│  ├───────────────────────┼──────┼───────┼──────┤        │
│  │ Hybrid Strength 4-Wk  │ Str  │  4    │ ⬆️   │        │
│  │ Push Pull Legs         │ Hyp  │  1    │ 🤖   │        │
│  └─────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────┘
                          │ click
                          ▼
┌─────────────────────────────────────────────────────────┐
│  PROGRAM DETAIL PAGE  (/vault/programs/:id)             │
├─────────────────────────────────────────────────────────┤
│  Name: Hybrid Strength 4-Week                           │
│  Goal: Build strength    Duration: 4 weeks              │
│  Equipment: Barbell, Pull-up Bar                        │
│  Source: UPLOADED                                       │
│                                                         │
│  [Delete] [Edit JSON] [Copy]                            │
│                                                         │
│  ▶ Week 1                                              │
│    ▶ Day 1 — Push (Hypertrophy)                        │
│      Warm-up: Arm Circles (30s each direction)          │
│      Section: Tier 1 Compound                           │
│        • Bench Press — 4×6-8 @ 80% 1RM (120s rest)     │
│      Cool-down: Chest Stretch (30s each side)           │
│    ▶ Day 2 — Pull (Hypertrophy)                        │
│  ▶ Week 2                                              │
│  ▶ Week 3                                              │
│  ▶ Week 4                                              │
└─────────────────────────────────────────────────────────┘
```

---

## 5. Requirements Status & Spec Mapping

### Legend

| Status | Meaning |
|--------|---------|
| ✅ | Fully implemented and tested |
| 🔨 | Partially implemented (some tasks remaining) |
| 📋 | Spec created, not yet implemented |
| 🚫 | Deferred / TODO |

### Requirements Completion Matrix

| # | Requirement | Status | Implementing Spec(s) |
|---|-------------|--------|---------------------|
| 1 | User Registration and Authentication | ✅ | `auth-service-mvp1` |
| 2 | Admin User Management | 📋 | `auth-service-mvp2` (requirements only) |
| 3 | AI-Powered Workout & Program Generation | 📋 | `workout-creator-service` (requirements only) |
| 4 | Workout and Program CRUD (Vault) | ✅ | `workout-creator-service-vault` |
| 5 | Vault Search and Filter | ✅ | `workout-creator-service-vault` |
| 6 | Active Workout — Theater Mode | 📋 | `workout-session-service` (requirements only) |
| 7 | Live Performance Logging | 📋 | `workout-session-service` (requirements only) |
| 8 | Session State and Program Progression | 📋 | `workout-session-service` (requirements only) |
| 9 | Progress Dashboard | 📋 | `progress-tracker-service` (requirements only) |
| 10 | CrossFit and Benchmark Tracking | 📋 | `progress-tracker-service` (requirements only) |
| 11 | Muscle Activation Heat Map | 📋 | `progress-tracker-service` (requirements only) |
| 12 | Workout Coach UI — Navigation & Home | ✅ | `workout-coach-ui-mvp1` |
| 13 | Data Integrity and Service Isolation | 🔨 | `platform` (requirements only; enforced in implemented services) |
| 14 | Schema Management and Database Standards | ✅ | Enforced across `auth-service-mvp1`, `workout-creator-service-upload`, `workout-creator-service-vault` |
| 15 | Workout and Program Upload (via UI) | ✅ | `workout-creator-service-upload` |
| 16 | Workout Ingest via Email | 🚫 | Deferred |
| 17 | Workout Photo Upload and AI Processing | 🚫 | Deferred |
| 18 | Workout Ingest via Email — Photo + JSON | 🚫 | Deferred |

### Detailed Spec → Implementation Mapping

#### ✅ Completed Specs (with tasks.md fully checked off)

| Spec | What It Delivers | Tasks |
|------|-----------------|-------|
| `auth-service-mvp1` | Registration, login, JWT (RS256), refresh tokens, bcrypt hashing, security filter, exception handling | 13 tasks — all ✅ |
| `workout-coach-ui-mvp1` | React SPA scaffold, auth context, login/register pages, home page, protected routing, API client with interceptors | 9 task groups — all ✅ |
| `workout-creator-service-upload` | Upload endpoint, validate endpoint, UploadParser, UploadFormatter, JPA persistence, frontend upload flow (file picker, preview, JSON editor) | 11 task groups — all ✅ |
| `workout-creator-service-vault` | Vault CRUD (list, get, update, delete, copy), search with filters, frontend vault pages, home menu restructure, 15 property-based tests, unit tests, integration tests | 17 task groups — 15 ✅, 1 in progress, 1 optional |

#### 📋 Specs with Requirements Only (no design/tasks yet)

| Spec | Scope | Blocked By |
|------|-------|-----------|
| `auth-service-mvp2` | Admin user management (list users, deactivate accounts) | Nothing — ready to spec |
| `workout-creator-service` (main) | AI generation via Gemini, round-trip property | Nothing — ready to spec |
| `workout-session-service` | Theater Mode, live logging, program progression, WebSocket | Workout Creator Service API (for fetching definitions) |
| `progress-tracker-service` | Dashboard, 1RM, benchmarks, heat map | Session Service (for `SessionCompleted` events) |
| `workout-coach-ui` (main) | Full UI spec including theater mode, progress views | Session + Progress services |
| `platform` | Cross-service contracts, data isolation rules | Reference spec — no implementation tasks |

### What's Left — Prioritised Backlog

```
Priority 1 (Ready Now):
  ├── AI Workout Generation (Gemini integration)
  ├── Admin User Management (auth-service-mvp2)
  └── Vault integration tests (task 15 — just completed)

Priority 2 (Depends on Generation):
  ├── Workout Session Service (Theater Mode)
  └── Session UI components

Priority 3 (Depends on Session):
  ├── Progress Tracker Service
  ├── Dashboard UI
  └── Benchmark tracking

Deferred (post-MVP):
  ├── Email ingest (Req 16)
  ├── Photo upload + AI extraction (Req 17)
  └── Email photo ingest (Req 18)
```

---

## Summary

**Built so far:**
- Full authentication system (register, login, JWT, refresh)
- React SPA with auth flows and protected routing
- Program upload with validation, preview, and JSON editing
- Complete Vault CRUD (list, get, update, delete, copy)
- Vault search with keyword, focus area, and modality filters
- Frontend vault pages (search, detail, JSON editor)
- Home page with expandable menus
- 15 property-based tests + unit tests + integration tests for vault
- Flyway migrations, hexagonal architecture throughout

**Next up:**
- AI workout generation (Gemini integration)
- Theater Mode (active workout execution)
- Progress dashboard and analytics
