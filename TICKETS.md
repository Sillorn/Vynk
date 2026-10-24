# Implementation backlog — Vynk

**63 tickets, M0–M6.** Dependency links refer to ticket IDs. No tickets have been created on GitHub and no implementation has started. `PROPOSED` tags are approval-gated pending Round 8.

## M0 — Foundation

### PN-001 — Initialize monorepo and minimal README

- **Priority:** P0 · **Relative size:** 2 points · **Decision:** CONFIRMED
- **Depends on:** None
- **Acceptance criteria:** Repository has frontend backend deploy and workflow folders; README explains prerequisites and startup.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-002 — Bootstrap Angular 22 standalone application

- **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-001
- **Acceptance criteria:** Angular app starts locally; strict TypeScript enabled; base routes compile.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-003 — Bootstrap Spring Boot 4.1.2 Java 25 service

- **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-001
- **Acceptance criteria:** Service starts on loopback; versioned API stub and health check respond.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-004 — Establish frontend design system and route shell

- **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-002
- **Acceptance criteria:** Angular Material theme; navigation shell; responsive layout; accessible baseline.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-005 — Implement backend module skeleton and shared error model

- **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-003
- **Acceptance criteria:** Feature packages exist; RFC9457 error DTO and trace ID standard used.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-006 — Implement GitHub Actions quality workflow

- **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-002, PN-003
- **Acceptance criteria:** Frontend and backend build and test on pull requests; failures block green status.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-007 — Establish API specification and contract conventions

- **Priority:** P0 · **Relative size:** 2 points · **Decision:** CONFIRMED
- **Depends on:** PN-005
- **Acceptance criteria:** OpenAPI generated locally; endpoint naming, pagination, validation errors documented.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

## M1 — Identity

### PN-008 — Configure persistent H2 database and Flyway

- **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-003
- **Acceptance criteria:** File database survives restart; Flyway runs; Hibernate validates; second process not supported.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-009 — Model accounts profiles and SQL integrity

- **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-008
- **Acceptance criteria:** User/profile tables; lowercase-letter username invariant; reserved-name and unique constraints; indexes.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-010 — Implement session auth and CSRF handling

- **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-009
- **Acceptance criteria:** Secure cookie, SameSite Lax; Angular XSRF header; login logout; 7-day idle setting tested.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-011 — Build username-password registration API

- **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-009, PN-010
- **Acceptance criteria:** Atomic account/profile creation; required name headline summary; validation and duplicate conflicts.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-012 — Build registration/login/logout Angular views

- **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-004, PN-011
- **Acceptance criteria:** Accessible forms and field errors; session restore and redirects.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-013 — Implement user settings password update and deletion

- **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-010, PN-011
- **Acceptance criteria:** Current-password verification; logout; confirmed transactional account deletion; backend tests.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-014 — Enforce authorization and data ownership

- **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-010, PN-011
- **Acceptance criteria:** 403 or privacy-preserving 404 for unauthorized operations; anonymous private access denied.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-015 — Implement auth throttling and security headers

- **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-010, PN-011
- **Acceptance criteria:** Login and registration rate limited; secure headers; private information excluded from logs.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-016 — Build auth integration and browser E2E coverage

- **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-012, PN-013, PN-014, PN-015
- **Acceptance criteria:** Register/login/logout, CSRF, expired session, password change, delete tested.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

## M2 — Profiles and CV

### PN-017 — Design full structured profile schema

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-009
- **Acceptance criteria:** Experience education certifications projects skills languages links dates and ordering migrated.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-018 — Implement public profile read endpoints

- **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-017, PN-014
- **Acceptance criteria:** Public DTO hides private data; profile by immutable username; optional empty sections omitted.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-019 — Implement profile section edit REST endpoints

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-017, PN-014
- **Acceptance criteria:** Owner-only CRUD, explicit save, validation for dates and lengths, ordering supported.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-020 — Build public profile page and avatar

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-004, PN-018
- **Acceptance criteria:** Readable responsive public page and generated initials avatar; no uploads.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-021 — Build section-based profile editor

- **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED
- **Depends on:** PN-004, PN-019
- **Acceptance criteria:** Reactive forms, save/cancel, error display, unsaved-change guard, reorder entries.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-022 — Build professional experience/education workflows

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-019, PN-021
- **Acceptance criteria:** Create edit delete reorder and ongoing role cases pass.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-023 — Build skills languages certifications projects workflows

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-019, PN-021
- **Acceptance criteria:** Structured editing, safe links, language proficiency, list ordering pass.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-024 — Implement on-demand ATS-friendly PDF CV service

- **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED
- **Depends on:** PN-018
- **Acceptance criteria:** Public PDF single-column multipage; no private/social data; Unicode and links validated.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-025 — Expose CV endpoint and download UI

- **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-020, PN-024
- **Acceptance criteria:** Public download with proper headers and filename; failure states handled.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-026 — Test profiles CV and ownership end-to-end

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-020, PN-021, PN-022, PN-023, PN-025
- **Acceptance criteria:** Complex profile save, date errors, unauthorized edits, CV rendering tested.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

## M3 — Social

### PN-027 — Model posts likes comments and constraints

- **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-008, PN-009
- **Acceptance criteria:** Post/comment/like migrations with ownership and cascading post deletion.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-028 — Implement posts CRUD and paginated feed APIs

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-027, PN-014
- **Acceptance criteria:** Public text posts; author edit/delete; all-feed and connections-feed chronology.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-029 — Build public and connections feed screens

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-004, PN-028
- **Acceptance criteria:** Paging, loading empty/error states, create/edit/delete owned posts.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-030 — Implement likes and flat comment APIs

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-027, PN-014
- **Acceptance criteria:** Single like toggle/user/post; comments flat; owner edit/delete; cascade tested.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-031 — Build post engagement UI

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-029, PN-030
- **Acceptance criteria:** Likes and comment updates accessible with optimistic or refreshed states.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-032 — Implement people search and directory API

- **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-018, PN-017
- **Acceptance criteria:** Case-insensitive name/headline/skill filtering; bounded pages, stable sort.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-033 — Build people directory and search UI

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-004, PN-032
- **Acceptance criteria:** Search and pagination, shareable profiles, accessible result cards.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-034 — Model connection request lifecycle and integrity

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-008, PN-009
- **Acceptance criteria:** Directed pending request and canonical accepted pair; no duplicates/self connections.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-035 — Implement connection-request and accepted-network APIs

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-034, PN-014
- **Acceptance criteria:** Send cancel accept decline disconnect; only eligible actor; status conflicts handled.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-036 — Build network invitations and connection screens

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-004, PN-035
- **Acceptance criteria:** Pending incoming/outgoing and accepted connections, relevant actions/feedback.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-037 — Test posts network search and permissions

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-028, PN-030, PN-032, PN-033, PN-035, PN-036
- **Acceptance criteria:** Cross-user workflows; direct endpoint abuse; pagination and cascade checks.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

## M4 — Messaging

### PN-038 — Model conversations messages and per-user state

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-008, PN-034
- **Acceptance criteria:** Unique unordered pair; read pointer; hidden state; messages immutable.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-039 — Implement message send and history APIs

- **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED
- **Depends on:** PN-038, PN-014, PN-035
- **Acceptance criteria:** Accepted connection required; persist before event; newest messages with older paging.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-040 — Build inbox and conversation UI

- **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED
- **Depends on:** PN-004, PN-039
- **Acceptance criteria:** List sorted by latest, read/unread states, send and load older messages.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-041 — Implement authenticated SSE event infrastructure

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-010, PN-039
- **Acceptance criteria:** Stream per browser tab; event type identifiers only; auth/cleanup/heartbeat.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-042 — Wire messages and unread updates through SSE

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-039, PN-041
- **Acceptance criteria:** Post-commit events notify recipient; REST refresh drives state, not event payload.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-043 — Implement SSE reconnection and reconciliation

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-040, PN-042
- **Acceptance criteria:** Backoff/retry; missed event recover by REST; no duplicate message on refresh.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-044 — Implement conversation hide/disconnect lifecycle

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-038, PN-039, PN-040
- **Acceptance criteria:** History retained after disconnect; new sends blocked; per-user hide separate.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-045 — Model and expose persistent notifications

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-008, PN-035
- **Acceptance criteria:** Notifications stored and paginated; mark read; private recipient only.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-046 — Build notification center and unread indicators

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-004, PN-046, PN-042
- **Acceptance criteria:** Notifications accessible and counts update via SSE; no push/email.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-047 — Add idempotent message retry and notification policies

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** PROPOSED
- **Depends on:** PN-039, PN-046
- **Acceptance criteria:** Same client request ID does not duplicate send; accepted notification events documented.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-048 — Messaging integration, SSE and authorization tests

- **Priority:** P1 · **Relative size:** 8 points · **Decision:** PROPOSED
- **Depends on:** PN-039, PN-040, PN-041, PN-042, PN-043, PN-044, PN-045, PN-046, PN-047
- **Acceptance criteria:** Two-user messaging and third-party denial, disconnect, reconnect, reload pass.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

## M5 — Quality and PWA

### PN-049 — Configure PWA app shell and installability

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-002, PN-004
- **Acceptance criteria:** Manifest, icons, SW; installable HTTPS; API and SSE never cached.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-050 — Implement PWA update and offline UX

- **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-049
- **Acceptance criteria:** Update prompt; offline shell loads, authenticated actions report no network.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-051 — Finalize frontend state and error treatment

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-029, PN-031, PN-033, PN-040, PN-047
- **Acceptance criteria:** All major views have consistent loading empty error retry states and navigation.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-052 — Accessibility audit and fixes

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-020, PN-021, PN-029, PN-031, PN-033, PN-040, PN-047
- **Acceptance criteria:** WCAG2.2 AA checks, keyboard tests and contrast/a11y regression work.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-053 — Security hardening and abuse test suite

- **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED
- **Depends on:** PN-016, PN-026, PN-037, PN-048
- **Acceptance criteria:** CSRF, session theft mitigation, authorization, rate limits, output escaping, cache privacy tests.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-054 — Add performance / capacity smoke scenarios

- **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-037, PN-048
- **Acceptance criteria:** Bounded feeds and connections, H2 and SSE resource tests with baseline reported.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-055 — Confirm proposed Round 8 behavior defaults

- **Priority:** P1 · **Relative size:** 2 points · **Decision:** PROPOSED
- **Depends on:** PN-016, PN-026, PN-037, PN-048
- **Acceptance criteria:** Product owner chooses Q116-137 defaults; amended tickets and acceptance cases linked.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-056 — Final integration E2E regression suite

- **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED
- **Depends on:** PN-050, PN-051, PN-052, PN-053, PN-054, PN-055
- **Acceptance criteria:** Critical anonymous/public/user/network/message/CV/PWA journeys automated.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

## M6 — Alpine release

### PN-057 — Create Alpine NGINX HTTPS deployment config

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-003, PN-049
- **Acceptance criteria:** TLS redirects; same-origin /api proxy; restricted backend; SSE buffering disabled.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-058 — Package backend and OpenRC non-root service

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-003, PN-008
- **Acceptance criteria:** Java 25 runtime validated on Alpine, env config, writable data directory, start/restart works.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-059 — Implement manual deployment and rollback scripts

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-057, PN-058
- **Acceptance criteria:** Versioned artifact install, health gating, service restart, frontend publish, rollback documented.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-060 — Implement and verify H2 online backup restore

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-008, PN-058
- **Acceptance criteria:** Consistent backup while running; restore tested offline; migrate only after backup.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-061 — Provide demo data fixtures and sanitization

- **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED
- **Depends on:** PN-009, PN-017, PN-027, PN-034, PN-038
- **Acceptance criteria:** Idempotent fictional users/posts/connections/conversations; no default production seed.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-062 — Complete minimal README and handoff checklist

- **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED
- **Depends on:** PN-059, PN-060, PN-061
- **Acceptance criteria:** Setup, test, deployment, recovery, security notes short and accurate.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

### PN-063 — Perform target-host final verification and release

- **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED
- **Depends on:** PN-056, PN-057, PN-058, PN-059, PN-060, PN-061, PN-062
- **Acceptance criteria:** Alpine end-to-end smoke, CV font validation, TLS cookies, SSE, service restart, backup restore verified.
- **Done when:** Automated checks appropriate to the change pass, errors and security cases are covered, and CI is green.

