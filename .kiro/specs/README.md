# HybridStrength — Spec Structure Overview

## Master Spec

| Spec | Description |
|------|-------------|
| `hybrid-strength-app` | Master requirements document covering the full platform. Source of truth for all requirements before they were split into subspecs. |

---

## Subspecs

Each subspec maps to a microservice or frontend application and is an independent feature spec with its own requirements, design, and tasks.

| Spec | Service | Requirements Coverage |
|------|---------|-----------------------|
| `auth-service-mvp1` | Auth_Service | User registration, login, JWT auth, refresh tokens, data ownership |
| `auth-service-mvp2` | Auth_Service | Admin user management, password reset |
| `workout-creator-service` | Workout_Creator_Service | AI workout/program generation via Gemini, Vault CRUD, search and filter |
| `workout-session-service` | Session_Service | Theater Mode, live performance logging, AMRAP lap counter, session state persistence, program progression |
| `progress-tracker-service` | Progress_Tracker_Service | Progress dashboard, 1RM estimation, PR notifications, CrossFit benchmark tracking, muscle activation heat map |
| `workout-coach-ui` | Workout_Coach_UI | Home screen navigation, Next Step indicator, authentication state, non-SPA constraint |
| `platform` | Cross-cutting | Inter-service data isolation contracts, API-only cross-service communication, RabbitMQ event delivery rules |

---

## Requirements Traceability

| Master Requirement | Subspec |
|--------------------|---------|
| Req 1: User Registration and Authentication | `auth-service-mvp1` → Req 1 |
| Req 2: Admin User Management | `auth-service-mvp2` → Req 1 |
| Req 3: AI-Powered Workout and Program Generation | `workout-creator-service` → Req 1 |
| Req 4: Workout and Program CRUD (Vault) | `workout-creator-service` → Req 2 |
| Req 5: Vault Search and Filter | `workout-creator-service` → Req 3 |
| Req 6: Active Workout — Theater Mode | `workout-session-service` → Req 1 |
| Req 7: Live Performance Logging | `workout-session-service` → Req 2 |
| Req 8: Session State and Program Progression | `workout-session-service` → Req 3 |
| Req 9: Progress Dashboard | `progress-tracker-service` → Req 1 |
| Req 10: CrossFit and Benchmark Tracking | `progress-tracker-service` → Req 2 |
| Req 11: Muscle Activation Heat Map | `progress-tracker-service` → Req 3 |
| Req 12: Workout Coach UI — Navigation and Home Screen | `workout-coach-ui` → Req 1 |
| Req 13: Data Integrity and Service Isolation | `platform` → Req 1 (+ per-service data ownership in each subspec) |
| Req 14: Schema Management and Database Standards | Distributed into each service subspec as a data ownership requirement |

---

## Spec Directory Structure

```
.kiro/specs/
├── hybrid-strength-app/        # Master spec (full platform)
│   ├── .config.kiro
│   └── requirements.md
├── auth-service/               # Auth Service subspec
│   ├── .config.kiro
│   └── requirements.md
├── workout-creator-service/    # Workout Creator Service subspec
│   ├── .config.kiro
│   └── requirements.md
├── workout-session-service/    # Workout Session Service subspec
│   ├── .config.kiro
│   └── requirements.md
├── progress-tracker-service/   # Progress Tracker Service subspec
│   ├── .config.kiro
│   └── requirements.md
├── workout-coach-ui/           # Workout Coach UI subspec
│   ├── .config.kiro
│   └── requirements.md
└── platform/                   # Cross-cutting integration contracts
    ├── .config.kiro
    └── requirements.md
```

---

## Next Steps

Each subspec is ready for design and task generation independently. Suggested order based on dependencies:

1. `platform` — establish integration contracts first
2. `auth-service` — all other services depend on JWT auth
3. `workout-creator-service` — session service depends on its API
4. `workout-session-service` — depends on workout creator API
5. `progress-tracker-service` — depends on SessionCompleted events
6. `workout-coach-ui` — depends on all backend services
