---
inclusion: always
---
# Project Guidelines

## Architecture
- Hexagonal architecture (ports and adapters)
- Microservice architecture — each domain (workout creator, workout session, progress tracker, user/auth) should be a separate service

## Backend
- Spring Boot for all microservices
- PostgreSQL as the database

## Frontend
- React 18 with Next.js (App Router, SSR-first)
- Not a Single Page Application — pages are server-rendered by default
- Client-side interactivity used only where required (e.g. Theater Mode timers, lap counters)
- TypeScript throughout
- Build tool: Vite (for component-level tooling); Next.js handles page bundling

## Additional Standards
- Full standards doc to be added
