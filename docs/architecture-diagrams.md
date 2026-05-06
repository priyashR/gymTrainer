# HybridStrength — Architecture Diagrams

This document contains Mermaid architecture diagrams covering the full HybridStrength platform: service interactions, hexagonal architecture per service, and database schema ownership.

---

## 1. High-Level Service Interaction Diagram

```mermaid
graph TB
    subgraph "Frontend"
        UI[Workout Coach UI<br/>React 18 SPA<br/>Vite + React Router v6]
    end

    subgraph "Backend Services"
        AUTH[Auth Service<br/>:8081<br/>Spring Boot 3.x]
        WCS[Workout Creator Service<br/>:8082<br/>Spring Boot 3.x]
        WSS[Workout Session Service<br/>:8083<br/>Spring Boot 3.x]
        PTS[Progress Tracker Service<br/>:8084<br/>Spring Boot 3.x]
    end

    subgraph "External Services"
        GEMINI[Google Gemini API<br/>AI Generation]
    end

    subgraph "Infrastructure"
        PG_AUTH[(PostgreSQL<br/>auth_service DB)]
        PG_WCS[(PostgreSQL<br/>workout_creator DB)]
        PG_WSS[(PostgreSQL<br/>workout_session DB)]
        PG_PTS[(PostgreSQL<br/>progress_tracker DB)]
        RMQ[RabbitMQ<br/>AMQP 0-9-1]
    end

    %% Frontend to Backend (REST over HTTPS)
    UI -->|"REST/JSON<br/>JWT Bearer"| AUTH
    UI -->|"REST/JSON<br/>JWT Bearer"| WCS
    UI -->|"REST/JSON<br/>JWT Bearer"| WSS
    UI -->|"WebSocket/STOMP<br/>Live Session"| WSS
    UI -->|"REST/JSON<br/>JWT Bearer"| PTS

    %% Inter-service REST calls
    WSS -->|"REST<br/>GET Workout/Program"| WCS

    %% External API
    WCS -->|"REST<br/>Resilience4j Circuit Breaker"| GEMINI

    %% Database ownership (exclusive)
    AUTH --- PG_AUTH
    WCS --- PG_WCS
    WSS --- PG_WSS
    PTS --- PG_PTS

    %% Async messaging via RabbitMQ
    WSS -->|"Publish<br/>SessionCompleted"| RMQ
    RMQ -->|"Consume<br/>SessionCompleted"| PTS
```

### Communication Patterns Summary

| Pattern | Usage |
|---------|-------|
| **REST (JSON/HTTP)** | All client→service and service→service synchronous calls |
| **JWT (RS256)** | Authentication on every protected endpoint; verified locally per service |
| **RabbitMQ (AMQP)** | Async domain events between services (e.g., `SessionCompleted`) |
| **WebSocket (STOMP)** | Real-time session updates from Workout Session Service to UI |
| **Circuit Breaker** | Resilience4j wrapping Gemini API calls (10s timeout) |

---

## 2. Hexagonal Architecture — Auth Service

```mermaid
graph TB
    subgraph "Auth Service"
        subgraph "Inbound Adapters"
            RC[RegistrationController<br/>POST /api/v1/auth/register]
            AC[AuthenticationController<br/>POST /api/v1/auth/login<br/>POST /api/v1/auth/refresh]
            JF[JwtAuthenticationFilter<br/>Security Filter Chain]
        end

        subgraph "Inbound Ports"
            RUU[RegisterUserUseCase]
            LU[LoginUseCase]
            RTU[RefreshTokenUseCase]
        end

        subgraph "Application Layer"
            RUS[RegisterUserService]
            LS[LoginService]
            RTS[RefreshTokenService]
        end

        subgraph "Domain"
            USER[User<br/>id, email, passwordHash, role]
            RT[RefreshToken<br/>id, tokenHash, userId, expiresAt]
            TP[TokenPair<br/>accessToken, refreshToken]
        end

        subgraph "Outbound Ports"
            UR[UserRepository]
            RTR[RefreshTokenRepository]
            PE[PasswordEncoder]
            TPR[TokenProvider]
        end

        subgraph "Outbound Adapters"
            JUR[JpaUserRepository<br/>→ SpringDataUserRepository]
            JRTR[JpaRefreshTokenRepository<br/>→ SpringDataRefreshTokenRepository]
            BPE[BcryptPasswordEncoder]
            JWT[JwtTokenProvider<br/>RS256 signing]
        end
    end

    subgraph "Infrastructure"
        PG[(PostgreSQL<br/>users, refresh_tokens)]
    end

    %% Flow
    RC --> RUU
    AC --> LU & RTU
    RUU -.-> RUS
    LU -.-> LS
    RTU -.-> RTS

    RUS --> UR & PE
    LS --> UR & PE & TPR & RTR
    RTS --> RTR & TPR

    UR -.-> JUR
    RTR -.-> JRTR
    PE -.-> BPE
    TPR -.-> JWT

    JUR --> PG
    JRTR --> PG
```

### Auth Service — Package Structure

```
authservice/
├── config/                    SecurityConfig, JwtConfig
├── registration/
│   ├── domain/                User
│   ├── ports/inbound/         RegisterUserUseCase
│   ├── ports/outbound/        UserRepository
│   ├── application/           RegisterUserService
│   └── adapters/
│       ├── inbound/           RegistrationController, dto/
│       └── outbound/          JpaUserRepository, SpringDataUserRepository, UserJpaEntity
├── authentication/
│   ├── domain/                RefreshToken, TokenPair
│   ├── ports/inbound/         LoginUseCase, RefreshTokenUseCase
│   ├── ports/outbound/        PasswordEncoder, RefreshTokenRepository, TokenProvider
│   ├── application/           LoginService, RefreshTokenService
│   └── adapters/
│       ├── inbound/           AuthenticationController, dto/
│       └── outbound/          BcryptPasswordEncoder, JpaRefreshTokenRepository,
│                              JwtTokenProvider, RefreshTokenJpaEntity, SpringDataRefreshTokenRepository
└── common/
    ├── dto/                   ErrorResponse, ValidationErrorResponse
    ├── exception/             GlobalExceptionHandler, DuplicateEmailException,
    │                          InvalidCredentialsException, InvalidRefreshTokenException
    └── security/              JwtAuthenticationFilter
```

---

## 3. Hexagonal Architecture — Workout Creator Service

```mermaid
graph TB
    subgraph "Workout Creator Service"
        subgraph "Inbound Adapters"
            UC[UploadController<br/>POST /api/v1/uploads/programs<br/>POST /api/v1/uploads/programs/validate]
            VC[VaultController<br/>GET/PUT/DELETE /api/v1/vault/programs<br/>POST .../copy<br/>GET .../search]
        end

        subgraph "Inbound Ports"
            UPU[UploadProgramUseCase]
            VPU[ValidateProgramUploadUseCase]
            LPU[ListProgramsUseCase]
            GPU[GetProgramUseCase]
            UPDU[UpdateProgramUseCase]
            DPU[DeleteProgramUseCase]
            CPU[CopyProgramUseCase]
            SPU[SearchProgramsUseCase]
        end

        subgraph "Application Layer"
            UPS[UploadProgramService]
            VPS[ValidateProgramUploadService]
            VS[VaultService]
        end

        subgraph "Domain"
            P[Program<br/>name, goal, durationWeeks,<br/>equipmentProfile]
            W[Week → Day → Section → Exercise]
            WCE[WarmCoolEntry]
            UP[UploadedProgram]
            PR[ParseResult]
            PARSER[UploadParser]
            FMT[UploadFormatter]
            VP[VaultProgram]
            VI[VaultItem]
            SC[SearchCriteria]
        end

        subgraph "Outbound Ports"
            UPR[UploadProgramRepository]
            VPR[VaultProgramRepository]
        end

        subgraph "Outbound Adapters"
            JUPR[JpaUploadProgramRepository]
            JVPR[JpaVaultProgramRepository]
            SDR[ProgramSpringDataRepository]
            ENT[JPA Entities<br/>ProgramJpaEntity, WeekJpaEntity,<br/>DayJpaEntity, SectionJpaEntity,<br/>ExerciseJpaEntity, WarmCoolEntryJpaEntity]
        end
    end

    subgraph "Infrastructure"
        PG[(PostgreSQL<br/>programs, weeks, days,<br/>sections, exercises,<br/>warm_cool_entries)]
    end

    %% Upload flow
    UC --> UPU & VPU
    UPU -.-> UPS
    VPU -.-> VPS
    UPS --> UPR
    UPR -.-> JUPR

    %% Vault flow
    VC --> LPU & GPU & UPDU & DPU & CPU & SPU
    LPU & GPU & UPDU & DPU & CPU & SPU -.-> VS
    VS --> VPR
    VPR -.-> JVPR

    %% Shared persistence
    JUPR --> SDR
    JVPR --> SDR
    SDR --> ENT
    ENT --> PG
```

### Workout Creator Service — Package Structure

```
workoutcreator/
├── config/                    SecurityConfig, JwtConfig, JwtProperties, UploadConfig
├── upload/
│   ├── domain/                UploadedProgram, ParseResult, UploadParser,
│   │                          UploadFormatter, UploadValidationError
│   ├── ports/inbound/         UploadProgramUseCase, ValidateProgramUploadUseCase
│   ├── ports/outbound/        UploadProgramRepository
│   ├── application/           UploadProgramService, ValidateProgramUploadService
│   └── adapters/
│       ├── inbound/           UploadController, dto/
│       └── outbound/          JpaUploadProgramRepository
├── vault/
│   ├── domain/                VaultProgram, VaultItem, SearchCriteria
│   ├── ports/inbound/         ListProgramsUseCase, GetProgramUseCase, UpdateProgramUseCase,
│   │                          DeleteProgramUseCase, CopyProgramUseCase, SearchProgramsUseCase
│   ├── ports/outbound/        VaultProgramRepository
│   ├── application/           VaultService
│   └── adapters/
│       ├── inbound/           VaultController, dto/
│       └── outbound/          JpaVaultProgramRepository, ProgramEntityMapper,
│                              ProgramSpringDataRepository, ProgramJpaEntity, WeekJpaEntity,
│                              DayJpaEntity, SectionJpaEntity, ExerciseJpaEntity, WarmCoolEntryJpaEntity
└── common/
    ├── model/                 Program, Week, Day, Section, Exercise, WarmCoolEntry,
    │                          ContentSource, Modality, ModalityType, SectionType
    ├── dto/                   ErrorResponse, ValidationErrorResponse
    ├── exception/             GlobalExceptionHandler, UploadValidationException,
    │                          ProgramAccessDeniedException
    └── security/              JwtAuthenticationFilter
```

---

## 4. Hexagonal Architecture — Workout Session Service (Planned)

```mermaid
graph TB
    subgraph "Workout Session Service"
        subgraph "Inbound Adapters"
            TC[TheaterController<br/>REST + WebSocket/STOMP<br/>Live session updates]
            LC[LoggingController<br/>POST /api/v1/sessions/log]
            PC[ProgressionController<br/>GET /api/v1/progression]
        end

        subgraph "Inbound Ports"
            STU[StartTheaterUseCase]
            LSU[LogSetUseCase]
            CSU[CompleteSessionUseCase]
            PPU[ProgramProgressionUseCase]
        end

        subgraph "Application Layer"
            TS[TheaterService]
            LGS[LoggingService]
            PS[ProgressionService]
        end

        subgraph "Domain"
            SS[Session<br/>id, userId, programId, status]
            SL[SetLog<br/>exercise, weight, reps, rpe]
            PP[ProgramProgress<br/>currentWeek, currentDay]
        end

        subgraph "Outbound Ports"
            SR[SessionRepository]
            EP[EventPublisher]
            WCC[WorkoutCreatorClient]
        end

        subgraph "Outbound Adapters"
            JSR[JpaSessionRepository]
            REP[RabbitEventPublisher<br/>→ SessionCompleted]
            RCC[RestWorkoutCreatorClient<br/>GET programs from WCS]
        end
    end

    subgraph "Infrastructure"
        PG[(PostgreSQL<br/>sessions, set_logs,<br/>program_progress)]
        RMQ[RabbitMQ]
        WCS_API[Workout Creator Service<br/>REST API]
    end

    TC --> STU & CSU
    LC --> LSU
    PC --> PPU

    STU & CSU -.-> TS
    LSU -.-> LGS
    PPU -.-> PS

    TS --> SR & EP & WCC
    LGS --> SR
    PS --> SR

    SR -.-> JSR
    EP -.-> REP
    WCC -.-> RCC

    JSR --> PG
    REP --> RMQ
    RCC --> WCS_API
```

---

## 5. Hexagonal Architecture — Progress Tracker Service (Planned)

```mermaid
graph TB
    subgraph "Progress Tracker Service"
        subgraph "Inbound Adapters"
            DC[DashboardController<br/>GET /api/v1/dashboard]
            BC[BenchmarkController<br/>GET /api/v1/benchmarks]
            HC[HeatmapController<br/>GET /api/v1/heatmap]
            SEL[SessionEventListener<br/>RabbitMQ Consumer]
        end

        subgraph "Inbound Ports"
            DU[DashboardUseCase]
            BU[BenchmarkUseCase]
            HU[HeatmapUseCase]
            PSE[ProcessSessionEventUseCase]
        end

        subgraph "Application Layer"
            DS[DashboardService]
            BS[BenchmarkService]
            HS[HeatmapService]
            SES[SessionEventService]
        end

        subgraph "Domain"
            WS[WorkoutStats<br/>volume, frequency, PRs]
            BM[Benchmark<br/>exercise, oneRepMax, date]
            MH[MuscleHeatmap<br/>muscleGroup, intensity, date]
        end

        subgraph "Outbound Ports"
            WSR[WorkoutStatsRepository]
            BMR[BenchmarkRepository]
            MHR[MuscleHeatmapRepository]
        end

        subgraph "Outbound Adapters"
            JWSR[JpaWorkoutStatsRepository]
            JBMR[JpaBenchmarkRepository]
            JMHR[JpaMuscleHeatmapRepository]
        end
    end

    subgraph "Infrastructure"
        PG[(PostgreSQL<br/>workout_stats, benchmarks,<br/>muscle_heatmap)]
        RMQ[RabbitMQ<br/>SessionCompleted events]
    end

    DC --> DU
    BC --> BU
    HC --> HU
    SEL --> PSE

    DU -.-> DS
    BU -.-> BS
    HU -.-> HS
    PSE -.-> SES

    DS --> WSR
    BS --> BMR
    HS --> MHR
    SES --> WSR & BMR & MHR

    WSR -.-> JWSR
    BMR -.-> JBMR
    MHR -.-> JMHR

    JWSR --> PG
    JBMR --> PG
    JMHR --> PG
    RMQ --> SEL
```

---

## 6. Frontend Architecture — Workout Coach UI

```mermaid
graph TB
    subgraph "Workout Coach UI (React 18 SPA)"
        subgraph "Routing (React Router v6)"
            ROUTES[App.tsx<br/>Route Definitions]
        end

        subgraph "Pages"
            HOME[Home.tsx]
            LOGIN[Login.tsx]
            REG[Register.tsx]
            UPLOAD[UploadPage.tsx]
            SEARCH[VaultSearchPage.tsx]
            DETAIL[ProgramDetailPage.tsx]
            CS[ComingSoon.tsx]
        end

        subgraph "Features"
            AUTH_F[features/auth/<br/>AuthContext, useAuth]
            UPLOAD_F[features/upload/<br/>FilePicker, JsonEditor,<br/>ProgramPreview, useUpload]
            VAULT_F[features/vault/<br/>VaultItemCard, ProgramJsonEditor,<br/>useVaultSearch, useProgram]
        end

        subgraph "Shared"
            COMP[components/<br/>ui/, layout/]
            LIB[lib/<br/>apiClient, authApi, vaultApi]
            TYPES[types/<br/>auth.ts, upload.ts, vault.ts]
            HOOKS[hooks/]
        end
    end

    subgraph "Backend APIs"
        AUTH_API[Auth Service<br/>/api/v1/auth/*]
        WCS_API[Workout Creator Service<br/>/api/v1/uploads/*<br/>/api/v1/vault/*]
        WSS_API[Workout Session Service<br/>/api/v1/sessions/*]
        PTS_API[Progress Tracker Service<br/>/api/v1/dashboard/*]
    end

    ROUTES --> HOME & LOGIN & REG & UPLOAD & SEARCH & DETAIL & CS

    AUTH_F --> LIB
    UPLOAD_F --> LIB
    VAULT_F --> LIB

    LIB -->|"REST + JWT"| AUTH_API
    LIB -->|"REST + JWT"| WCS_API
    LIB -->|"REST + JWT"| WSS_API
    LIB -->|"REST + JWT"| PTS_API
```

### Frontend Route Map

| Route | Page | Status |
|-------|------|--------|
| `/login` | Login | ✅ Implemented |
| `/register` | Register | ✅ Implemented |
| `/` | Home | ✅ Implemented |
| `/upload` | UploadPage | ✅ Implemented |
| `/new-workout` | ComingSoon | Placeholder |
| `/my-performance` | ComingSoon | Placeholder |
| `/workout` | ComingSoon | Placeholder |
| `/vault/search` | VaultSearchPage | Planned |
| `/vault/programs/:id` | ProgramDetailPage | Planned |
| `/workout/continue` | ComingSoon | Planned |

---

## 7. Database Schema Ownership

```mermaid
graph LR
    subgraph "Auth Service DB (V001–V099)"
        USERS[users<br/>─────────<br/>id UUID PK<br/>email VARCHAR UNIQUE<br/>password_hash VARCHAR<br/>role VARCHAR<br/>created_at TIMESTAMPTZ<br/>updated_at TIMESTAMPTZ]
        REFRESH[refresh_tokens<br/>─────────<br/>id UUID PK<br/>token_hash VARCHAR UNIQUE<br/>user_id UUID FK→users<br/>expires_at TIMESTAMPTZ<br/>created_at TIMESTAMPTZ]
    end

    subgraph "Workout Creator Service DB (V100–V199)"
        PROGRAMS[programs<br/>─────────<br/>id UUID PK<br/>name VARCHAR<br/>duration_weeks INT<br/>goal VARCHAR<br/>equipment_profile TEXT<br/>owner_user_id VARCHAR<br/>content_source VARCHAR<br/>created_at TIMESTAMPTZ<br/>updated_at TIMESTAMPTZ]
        WEEKS[weeks<br/>─────────<br/>id UUID PK<br/>program_id UUID FK→programs<br/>week_number INT]
        DAYS[days<br/>─────────<br/>id UUID PK<br/>week_id UUID FK→weeks<br/>day_number INT<br/>day_label VARCHAR<br/>focus_area VARCHAR<br/>modality VARCHAR<br/>methodology_source VARCHAR]
        SECTIONS[sections<br/>─────────<br/>id UUID PK<br/>day_id UUID FK→days<br/>name VARCHAR<br/>section_type VARCHAR<br/>format VARCHAR<br/>time_cap INT<br/>sort_order INT]
        EXERCISES[exercises<br/>─────────<br/>id UUID PK<br/>section_id UUID FK→sections<br/>exercise_name VARCHAR<br/>modality_type VARCHAR<br/>prescribed_sets INT<br/>prescribed_reps VARCHAR<br/>prescribed_weight VARCHAR<br/>rest_interval_seconds INT<br/>notes TEXT<br/>sort_order INT]
        WARMCOOL[warm_cool_entries<br/>─────────<br/>id UUID PK<br/>day_id UUID FK→days<br/>entry_type VARCHAR<br/>movement VARCHAR<br/>instruction TEXT<br/>sort_order INT]
    end

    subgraph "Workout Session Service DB (V200–V299)"
        SESSIONS[sessions<br/>─────────<br/>id UUID PK<br/>user_id VARCHAR<br/>program_id UUID<br/>status VARCHAR<br/>started_at TIMESTAMPTZ<br/>completed_at TIMESTAMPTZ]
        SETLOGS[set_logs<br/>─────────<br/>id UUID PK<br/>session_id UUID FK→sessions<br/>exercise_name VARCHAR<br/>set_number INT<br/>weight DECIMAL<br/>reps INT<br/>rpe DECIMAL<br/>logged_at TIMESTAMPTZ]
        PROGRESS[program_progress<br/>─────────<br/>id UUID PK<br/>user_id VARCHAR<br/>program_id UUID<br/>current_week INT<br/>current_day INT<br/>updated_at TIMESTAMPTZ]
    end

    subgraph "Progress Tracker Service DB (V300–V399)"
        STATS[workout_stats<br/>─────────<br/>id UUID PK<br/>user_id VARCHAR<br/>total_volume DECIMAL<br/>session_count INT<br/>period_start DATE<br/>period_end DATE]
        BENCHMARKS[benchmarks<br/>─────────<br/>id UUID PK<br/>user_id VARCHAR<br/>exercise_name VARCHAR<br/>one_rep_max DECIMAL<br/>achieved_at TIMESTAMPTZ]
        HEATMAP[muscle_heatmap<br/>─────────<br/>id UUID PK<br/>user_id VARCHAR<br/>muscle_group VARCHAR<br/>intensity DECIMAL<br/>recorded_at TIMESTAMPTZ]
    end

    %% Relationships within Auth Service
    USERS --> REFRESH

    %% Relationships within Workout Creator Service
    PROGRAMS --> WEEKS
    WEEKS --> DAYS
    DAYS --> SECTIONS
    DAYS --> WARMCOOL
    SECTIONS --> EXERCISES

    %% Relationships within Workout Session Service
    SESSIONS --> SETLOGS
```

### Schema Ownership Rules

| Service | Migration Range | Tables Owned |
|---------|----------------|--------------|
| Auth Service | V001–V099 | `users`, `refresh_tokens` |
| Workout Creator Service | V100–V199 | `programs`, `weeks`, `days`, `sections`, `exercises`, `warm_cool_entries` |
| Workout Session Service | V200–V299 | `sessions`, `set_logs`, `program_progress` |
| Progress Tracker Service | V300–V399 | `workout_stats`, `benchmarks`, `muscle_heatmap` |

**Key constraint:** No service reads from or writes to another service's tables. Cross-service data access goes through REST APIs or RabbitMQ events.

---

## 8. Deployment Topology

```mermaid
graph TB
    subgraph "Client"
        BROWSER[Browser<br/>React SPA]
    end

    subgraph "Kubernetes Cluster (dev namespace)"
        subgraph "Services"
            AUTH_POD[auth-service<br/>:8081]
            WCS_POD[workout-creator-service<br/>:8082]
            WSS_POD[workout-session-service<br/>:8083]
            PTS_POD[progress-tracker-service<br/>:8084]
        end

        subgraph "Data Layer"
            PG[PostgreSQL 16<br/>:5432<br/>Separate DBs per service]
            RABBIT[RabbitMQ 3.13+<br/>:5672 / :15672]
        end
    end

    subgraph "External"
        GEMINI[Google Gemini API]
    end

    BROWSER -->|HTTPS| AUTH_POD
    BROWSER -->|HTTPS| WCS_POD
    BROWSER -->|HTTPS + WS| WSS_POD
    BROWSER -->|HTTPS| PTS_POD

    AUTH_POD --> PG
    WCS_POD --> PG
    WCS_POD --> GEMINI
    WSS_POD --> PG
    WSS_POD --> RABBIT
    WSS_POD -->|REST| WCS_POD
    PTS_POD --> PG
    PTS_POD --> RABBIT
```

---

## 9. Event Flow — Session Completion

```mermaid
sequenceDiagram
    participant UI as Workout Coach UI
    participant WSS as Workout Session Service
    participant RMQ as RabbitMQ
    participant PTS as Progress Tracker Service

    UI->>WSS: POST /api/v1/sessions/{id}/complete
    WSS->>WSS: Mark session as completed
    WSS->>WSS: Calculate session stats
    WSS->>RMQ: Publish SessionCompleted event
    WSS-->>UI: 200 OK (session summary)

    RMQ->>PTS: Deliver SessionCompleted
    PTS->>PTS: Update workout_stats
    PTS->>PTS: Check for new PRs → update benchmarks
    PTS->>PTS: Update muscle_heatmap
    PTS-->>RMQ: ACK
```

---

## 10. Authentication Flow

```mermaid
sequenceDiagram
    participant UI as Workout Coach UI
    participant AUTH as Auth Service
    participant WCS as Workout Creator Service

    Note over UI,AUTH: Registration
    UI->>AUTH: POST /api/v1/auth/register {email, password}
    AUTH->>AUTH: Validate, hash password (bcrypt cost 12)
    AUTH-->>UI: 201 Created {userId}

    Note over UI,AUTH: Login
    UI->>AUTH: POST /api/v1/auth/login {email, password}
    AUTH->>AUTH: Verify credentials
    AUTH-->>UI: 200 OK {accessToken} + Set-Cookie: refreshToken (HttpOnly)

    Note over UI,WCS: Authenticated API Call
    UI->>WCS: GET /api/v1/vault/programs<br/>Authorization: Bearer {accessToken}
    WCS->>WCS: Verify JWT locally (RS256 public key)
    WCS->>WCS: Extract userId from JWT subject
    WCS-->>UI: 200 OK {programs}

    Note over UI,AUTH: Token Refresh
    UI->>AUTH: POST /api/v1/auth/refresh<br/>Cookie: refreshToken
    AUTH->>AUTH: Validate refresh token hash
    AUTH-->>UI: 200 OK {accessToken} + Set-Cookie: refreshToken (rotated)
```
