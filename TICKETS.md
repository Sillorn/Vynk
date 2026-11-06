# Implementation backlog — Vynk

**64 tickets, M0–M6.** Ticket details and statuses below match `TICKETS.csv`. Dependency links refer to ticket IDs. `PROPOSED` decisions are approval-gated pending Round 8.

## M0 — Foundation

### PN-001 — Initialize monorepo and minimal README

- **Type:** Task · **Priority:** P0 · **Relative size:** 2 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** None
- **Acceptance criteria:** Repository has frontend backend deploy and workflow folders; README explains prerequisites and startup

### PN-002 — Bootstrap Angular 22 standalone application

- **Type:** Task · **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-001
- **Acceptance criteria:** Angular app starts locally; strict TypeScript enabled; base routes compile

### PN-003 — Bootstrap Spring Boot 4.1.2 Java 25 service

- **Type:** Task · **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-001
- **Acceptance criteria:** Service starts on loopback; versioned API stub and health check respond

### PN-004 — Establish frontend design system and route shell

- **Type:** Task · **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-002
- **Acceptance criteria:** Angular Material theme; navigation shell; responsive layout; accessible baseline

### PN-005 — Implement backend module skeleton and shared error model

- **Type:** Story · **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-003
- **Acceptance criteria:** Feature packages exist; RFC9457 error DTO and trace ID standard used

### PN-006 — Implement GitHub Actions quality workflow

- **Type:** Story · **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-002, PN-003
- **Acceptance criteria:** Frontend and backend build and test on pull requests; failures block green status

### PN-007 — Establish API specification and contract conventions

- **Type:** Task · **Priority:** P0 · **Relative size:** 2 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-005
- **Acceptance criteria:** OpenAPI generated locally; endpoint naming, pagination, validation errors documented

## M1 — Identity

### PN-008 — Configure persistent H2 database and Flyway

- **Type:** Task · **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-003
- **Acceptance criteria:** File database survives restart; Flyway runs; Hibernate validates; second process not supported

### PN-009 — Model accounts profiles and SQL integrity

- **Type:** Story · **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-008
- **Acceptance criteria:** User/profile tables; lowercase-letter username invariant; reserved-name and unique constraints; indexes

### PN-010 — Implement session auth and CSRF handling

- **Type:** Story · **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-009
- **Acceptance criteria:** Secure cookie, SameSite Lax; Angular XSRF header; login logout; 7-day idle setting tested

### PN-011 — Build username-password registration API

- **Type:** Story · **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-009, PN-010
- **Acceptance criteria:** Atomic account/profile creation; required name headline summary; validation and duplicate conflicts

### PN-012 — Build registration/login/logout Angular views

- **Type:** Story · **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-004, PN-011
- **Acceptance criteria:** Accessible forms and field errors; session restore and redirects

### PN-013 — Implement user settings password update and deletion

- **Type:** Story · **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-010, PN-011
- **Acceptance criteria:** Current-password verification; logout; confirmed transactional account deletion/session invalidation; backend tests

### PN-014 — Enforce authorization and data ownership

- **Type:** Story · **Priority:** P0 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-010, PN-011
- **Acceptance criteria:** 403 or privacy-preserving 404 for unauthorized operations; anonymous private access denied

### PN-015 — Implement auth throttling and security headers

- **Type:** Story · **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-010, PN-011
- **Acceptance criteria:** Login and registration rate limited; secure headers; private information excluded from logs

### PN-016 — Complete auth API integration coverage

- **Type:** Story · **Priority:** P0 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-012, PN-013, PN-014, PN-015
- **Acceptance criteria:** API tests cover registration, login/logout, CSRF, invalid session, password change and account deletion/session invalidation; browser E2E deferred to PN-056 review

## M2 — Profiles and CV

### PN-017 — Design full structured profile schema

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-009
- **Acceptance criteria:** Experience education certifications projects skills languages links dates and ordering migrated

### PN-018 — Implement public profile read endpoints

- **Type:** Story · **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-017, PN-014
- **Acceptance criteria:** Public DTO hides private data; profile by immutable username; optional empty sections omitted

### PN-019 — Implement profile section edit REST endpoints

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-017, PN-014
- **Acceptance criteria:** Owner-only CRUD, explicit save, validation for dates and lengths, ordering supported

### PN-020 — Build public profile page and avatar

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-004, PN-018
- **Acceptance criteria:** Readable responsive public page and generated initials avatar; no uploads

### PN-021 — Build section-based profile editor

- **Type:** Story · **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-004, PN-019
- **Acceptance criteria:** Reactive forms, save/cancel, unsaved-change guard, reorder entries

### PN-022 — Implement professional experience/education workflows

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-019, PN-021
- **Acceptance criteria:** Create edit delete reorder and ongoing role cases pass

### PN-023 — Build skills languages certifications projects workflows

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-019, PN-021
- **Acceptance criteria:** Create edit delete reorder; safe links, language proficiency, list ordering pass

### PN-024 — Implement on-demand ATS-friendly PDF CV service

- **Type:** Story · **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-018
- **Acceptance criteria:** Public PDF single-column multipage; no private/social data; Unicode and links validated

### PN-025 — Expose CV endpoint and download UI

- **Type:** Story · **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-020, PN-024
- **Acceptance criteria:** Public download with proper headers and filename; failure states handled

### PN-026 — Test profiles CV and ownership end-to-end

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-020, PN-021, PN-022, PN-023, PN-025
- **Acceptance criteria:** Complex profile save, date errors, unauthorized edits, CV rendering tested

## M3 — Social

### PN-027 — Model posts likes comments and constraints

- **Type:** Story · **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-008, PN-009
- **Acceptance criteria:** Post/comment/like migrations with ownership and cascading post deletion

### PN-028 — Implement posts CRUD and paginated feed APIs

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-027, PN-014
- **Acceptance criteria:** Public text posts; author edit/delete; all-feed and connections-feed chronology

### PN-029 — Build public and connections feed screens

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-004, PN-028
- **Acceptance criteria:** Paging, loading empty/error states, create/edit/delete owned posts

### PN-030 — Implement likes and flat comment APIs

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-027, PN-014
- **Acceptance criteria:** Single like toggle/user/post; comments flat; owner edit/delete; cascade tested

### PN-031 — Build post engagement UI

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-029, PN-030
- **Acceptance criteria:** Likes and comment updates accessible with optimistic or refreshed states

### PN-032 — Implement people search and directory API

- **Type:** Story · **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-018, PN-017
- **Acceptance criteria:** Case-insensitive name/headline/skill filtering; bounded pages, stable sort

### PN-033 — Build people directory and search UI

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-004, PN-032
- **Acceptance criteria:** Search and pagination, shareable profiles, accessible result cards

### PN-034 — Model connection request lifecycle and integrity

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-008, PN-009
- **Acceptance criteria:** Directed pending request and canonical accepted pair; no duplicates/self connections

### PN-035 — Implement connection-request and accepted-network APIs

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-034, PN-014
- **Acceptance criteria:** Send cancel accept decline disconnect; only eligible actor; status conflicts handled

### PN-036 — Build network invitations and connection screens

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-004, PN-035
- **Acceptance criteria:** Pending incoming/outgoing and accepted connections, relevant actions/feedback

### PN-037 — Test posts network search and permissions

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-028, PN-030, PN-032, PN-033, PN-035, PN-036
- **Acceptance criteria:** Cross-user workflows; direct endpoint abuse; pagination and cascade checks

## M4 — Messaging

### PN-038 — Model conversations messages and per-user state

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-008, PN-034
- **Acceptance criteria:** Unique unordered pair; read pointer; hidden state; messages immutable

### PN-039 — Implement message send and history APIs

- **Type:** Story · **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-038, PN-014, PN-035
- **Acceptance criteria:** Accepted connection required; persist before event; newest messages with older paging

### PN-040 — Build inbox and conversation UI

- **Type:** Story · **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-004, PN-039
- **Acceptance criteria:** List sorted by latest, read/unread states, send and load older messages

### PN-041 — Implement authenticated SSE event infrastructure

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-010, PN-039
- **Acceptance criteria:** Stream per browser tab; event type identifiers only; auth/cleanup/heartbeat

### PN-042 — Wire messages and unread updates through SSE

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-039, PN-041
- **Acceptance criteria:** Post-commit events notify recipient; REST refresh drives state, not event payload

### PN-043 — Implement SSE reconnection and reconciliation

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-040, PN-042
- **Acceptance criteria:** Backoff/retry; missed event recover by REST; no duplicate message on refresh

### PN-044 — Implement conversation hide/disconnect lifecycle

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-038, PN-039, PN-040
- **Acceptance criteria:** History retained after disconnect; new sends blocked; per-user hide separate

### PN-045 — Model and expose persistent notifications

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-008, PN-035
- **Acceptance criteria:** Notifications stored and paginated; mark read; private recipient only

### PN-046 — Build notification center and unread indicators

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-004, PN-046, PN-042
- **Acceptance criteria:** Notifications accessible and counts update via SSE; no push/email

### PN-047 — Add idempotent message retry and notification policies

- **Type:** Task · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-039, PN-046
- **Acceptance criteria:** Same client request ID does not duplicate send; accepted notification events documented

### PN-048 — Messaging integration, SSE and authorization tests

- **Type:** Story · **Priority:** P1 · **Relative size:** 8 points · **Decision:** PROPOSED · **Status:** Backlog
- **Depends on:** PN-039, PN-040, PN-041, PN-042, PN-043, PN-044, PN-045, PN-046
- **Acceptance criteria:** Two-user messaging and third-party denial, disconnect, reconnect, reload pass

## M5 — Quality and PWA

### PN-049 — Configure PWA app shell and installability

- **Type:** Task · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-002, PN-004
- **Acceptance criteria:** Manifest, icons, SW; API and SSE never cached

### PN-050 — Implement PWA update and offline UX

- **Type:** Story · **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-049
- **Acceptance criteria:** Update prompt; offline shell loads, authenticated actions report no network

### PN-051 — Finalize frontend state and error treatment

- **Type:** Task · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-029, PN-031, PN-033, PN-040, PN-047
- **Acceptance criteria:** All major views have consistent loading empty error retry states and navigation

### PN-052 — Accessibility audit and fixes

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-020, PN-021, PN-029, PN-031, PN-033, PN-040, PN-047
- **Acceptance criteria:** WCAG2.2 AA checks, keyboard tests and contrast/a11y regression work

### PN-053 — Security hardening and abuse test suite

- **Type:** Story · **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-016, PN-026, PN-037, PN-048
- **Acceptance criteria:** CSRF, session theft mitigation, authorization, rate limits, output escaping, cache privacy tests

### PN-054 — Add performance / capacity smoke scenarios

- **Type:** Task · **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Backlog
- **Depends on:** PN-037, PN-048
- **Acceptance criteria:** Bounded feeds and connections, H2 and SSE resource tests with baseline reported

### PN-055 — Confirm proposed Round 8 behavior defaults

- **Type:** Task · **Priority:** P1 · **Relative size:** 2 points · **Decision:** PROPOSED · **Status:** Backlog
- **Depends on:** PN-016, PN-026, PN-037, PN-048
- **Acceptance criteria:** Product owner chooses Q116-137 defaults; amended tickets and acceptance cases linked

### PN-056 — Final integration E2E regression suite

- **Type:** Story · **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED · **Status:** Backlog
- **Depends on:** PN-050, PN-051, PN-052, PN-053, PN-054, PN-055
- **Acceptance criteria:** Critical anonymous/public/user/network/message/CV/PWA journeys automated

## M6 — Alpine release

### PN-057 — Create Alpine NGINX HTTPS deployment config

- **Type:** Task · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-003, PN-049
- **Acceptance criteria:** TLS redirects; same-origin /api proxy; restricted backend; SSE buffering disabled

### PN-058 — Package backend and OpenRC non-root service

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-003, PN-008
- **Acceptance criteria:** Java 25 runtime validated on Alpine, env config, writable data directory, start/restart works

### PN-059 — Implement manual deployment and rollback scripts

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-057, PN-058
- **Acceptance criteria:** Versioned artifact install, health gating, service restart, frontend publish, rollback documented

### PN-060 — Document operational release and recovery runbooks

- **Type:** Task · **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-057, PN-058, PN-059
- **Acceptance criteria:** Backup restore, rollback, health checks and incident steps documented

### PN-061 — Add CI release artifact and provenance

- **Type:** Task · **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-006, PN-058
- **Acceptance criteria:** Build artifacts checksummed; dependency provenance retained; no automatic deploy

### PN-062 — Review scope and prepare production-readiness handoff

- **Type:** Task · **Priority:** P1 · **Relative size:** 3 points · **Decision:** CONFIRMED · **Status:** Backlog
- **Depends on:** PN-056, PN-060, PN-061
- **Acceptance criteria:** Known limits and security posture documented; remaining items prioritized

### PN-063 — Perform target-host final verification and release

- **Type:** Task · **Priority:** P1 · **Relative size:** 8 points · **Decision:** CONFIRMED · **Status:** Backlog
- **Depends on:** PN-056, PN-057, PN-058, PN-059, PN-060, PN-061, PN-062
- **Acceptance criteria:** Alpine end-to-end smoke, CV font validation, TLS cookies, SSE, service restart, backup restore verified

## M3 — Social

### PN-064 — Define post audience and enforce feed visibility

- **Type:** Story · **Priority:** P1 · **Relative size:** 5 points · **Decision:** CONFIRMED · **Status:** Done
- **Depends on:** PN-027, PN-028, PN-029
- **Acceptance criteria:** Posts have explicit Public or Connections audience selected in composer; public feed returns only Public posts; connections feed returns Public posts and posts by accepted connections with Connections audience; authors can see their own posts in either view; edits preserve audience; migration assigns existing posts Public; API and UI tests verify unauthorized users cannot read connection-only posts or engage with them
