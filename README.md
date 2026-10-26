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
- Backend: `cd backend && ./mvnw spring-boot:run` — binds to loopback, uses H2 file DB

The backend uses the committed Maven wrapper, so a separate Maven install is not required.

## Quality checks (local)
- Frontend: `cd frontend && npm run format:check && npm run build && npm run test:ci`
- Backend: `cd backend && ./mvnw verify`

## Continuous integration
`.github/workflows/quality.yml` runs the frontend format/build checks and the backend
`./mvnw verify -DskipTests` build on every pull request and on pushes to `main`. Unit test steps
currently run as non-blocking (`continue-on-error`) while the identity/profile suites are brought
up to date; build or format failures still fail the workflow. To block merges when the workflow
fails, enable branch protection for `main` and require the **Quality** status checks.

## Notes
- No Docker, no separate PostgreSQL. Frontend served by NGINX in deployment; backend is an OpenRC service on Alpine.
- Auth is session-cookie + CSRF; SSE for notifications/messages (REST fallback/reconnect).
- See `PROJECT_PLAN.md` for full decisions and constraints and `TICKETS.md` for the delivery backlog.

## Alpine deployment

Deployment is manual; CI does not publish or deploy artifacts. Build the backend JAR and frontend distribution, then transfer both to the Alpine host. Install the service/configuration once with `deploy/alpine/scripts/install-service.sh`, configure `/etc/vynk/vynk.env`, and provision NGINX/TLS using `deploy/alpine/nginx/vynk.conf`. Keep the backend loopback-only and the environment file root-readable only.

Deploy as root with `deploy/alpine/scripts/deploy-release.sh VERSION /path/to/vynk.jar /path/to/browser-dist`. The script retains versioned artifacts under `/opt/vynk/releases`, switches the service JAR and frontend root, restarts OpenRC, and gates success on the local Actuator health endpoint. On failure, it restores the previously active links and restarts the service. To manually restore a retained release, run `deploy/alpine/scripts/rollback-release.sh VERSION`. Keep old release directories until the new version is verified; these scripts do not remove releases or replace database backup/migration procedures.
