# AGENTS.md

## Repo layout
- `frontend/` — Angular 22 (standalone, Material, signals/RxJS), built with `@angular/build`. PWA setup per plan.
- `backend/` — Spring Boot 4.1.2 / Java 25 modular monolith under `/api/v1`. Uses Spring Data JPA + Flyway + H2 file DB. OpenAPI at runtime via `springdoc` (UI disabled; docs generation allowed locally/CI).
- `deploy/alpine/` — NGINX/OpenRC configs, scripts, env examples (Alpine deployment).
- `.github/workflows/quality.yml` — CI only (build/lint/test). No auto-deploy.

## Commands
**Frontend (run from `frontend/`):**
- `npm ci` — install deps (Node 22+, npm per packageManager)
- `npm start` — `ng serve` (dev server)
- `npm run build` — `ng build` (production by default)
- `npm run test` / `npm run test:ci` — Vitest via `@angular/build:unit-test` (`test:ci` runs with `--watch=false`)
- `npm run format:check` / `npm run format` — Prettier

**Backend (run from `backend/`):**
- `./mvnw spring-boot:run` — dev server on `127.0.0.1:8080` (uses wrapper; no global Maven required)
- `./mvnw -B -ntp verify` — build + tests (CI uses this)
- Run single test: `./mvnw -Dtest=ClassName test` or `./mvnw -Dtest=ClassName#method test`
- Package: `./mvnw package` (produces JAR via Spring Boot plugin)

**CI parity (from repo root):** Frontend does `format:check && build && test:ci`; backend does `./mvnw -B -ntp verify`.

## Architecture notes
- API base `/api/v1` (see `shared/api/ApiPaths`). Errors follow RFC 9457 (`shared/error/*`), with trace IDs via `shared/tracing/*`.
- Auth: session cookies (HttpOnly, SameSite=Lax) + CSRF (Angular XSRF). Security config in `shared/security/SecurityConfig`. User details in `auth/UserDetailsServiceImpl`. 
- Persistence: H2 file `jdbc:h2:file:./data/vynk` (prod/dev), Flyway migrations in `src/main/resources/db/migration` (e.g. `V1__...`, `V2__accounts_profiles.sql`), `ddl-auto: validate`. One backend instance owns the DB.
- Identity model in `identity/` (`UserAccount`, `Profile`) with SQL constraints/triggers for lowercase usernames and reserved names.
- Frontend served by NGINX in deployment; backend behind reverse proxy on loopback.

## Gotchas / constraints
- Java 25 required (set in `pom.xml`). Spring Boot 4.1.2.
- Backend binds to `127.0.0.1` only in config; Actuator `health` only exposed; `/v3/api-docs` disabled in deployed env via `VYNK_API_DOCS_ENABLED=false`.
- Frontend tests use Vitest (`jsdom`) not Karma. Use `test:ci` in CI/local non-watch runs.
- Do not add Docker/Postgres/admin/email flows — see `PROJECT_PLAN.md` for boundaries.
- Follow existing package structure by feature (`identity`, `auth`, `shared/*`, etc.). Use the RFC 9457 error model for API errors.
- Tickets in `TICKETS.md`/`PROJECT_PLAN.md` are the roadmap; only implement what's asked for. `PROPOSED` tickets need approval.
- Once ticket is done, update Status to "Done" in @TICKETS.csv