# Vynk

Vynk is a professional networking demo — a monorepo for an original, LinkedIn-inspired application.

## Structure
- `frontend/` — Angular 22 (standalone) PWA
- `backend/` — Spring Boot 4.1.2 / Java 25 modular monolith (API under `/api/v1`)
- `deploy/alpine/` — NGINX/OpenRC configs, scripts, example env for Alpine Linux
- `.github/workflows/` — CI (build/lint/test; no auto-deploy)
- `docs/` — supporting docs

## Prerequisites
- Node.js 22+ (for Angular 22)
- JDK 25
- Git

## Startup (local)
- Frontend: `cd frontend && npm ci && npm start` — serves on `http://localhost:4200/`
- Backend: `cd backend && mvn spring-boot:run` — binds to loopback, uses H2 file DB

## Notes
- No Docker, no separate PostgreSQL. Frontend served by NGINX in deployment; backend is an OpenRC service on Alpine.
- Auth is session-cookie + CSRF; SSE for notifications/messages (REST fallback/reconnect).
- See `PROJECT_PLAN.md` for full decisions and constraints and `TICKETS.md` for the delivery backlog.
