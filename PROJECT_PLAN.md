# Professional Network Demo — Full Project Plan

**Status:** Planning deliverable; implementation not started.  
**Baseline:** Discovery rounds 1–7 and accepted decisions in round 8's predecessor context, dated 24 October 2026.  
**Approval:** User's instruction to create the plan authorizes documentation and ticket preparation, **not implementation**.

## 1. Executive summary

Build an original LinkedIn-inspired, multi-user professional networking demonstration. Visitors can discover public professional profiles, browse a chronological public feed, and download PDF CVs. Registered users can create comprehensive profiles, publish text posts, like/comment, form mutual professional connections, and exchange private messages with connected users. The solution should be polished enough for nontechnical audiences and demonstrate clean full-stack engineering practices to technical reviewers.

### Success outcomes
1. A stranger can visit the public feed, find a person, inspect their profile and export a readable PDF CV without signing in.
2. A user can register with a username and password plus nonempty professional identity, sign in, edit a structured profile and publish posts.
3. Two users can connect by invitation and then exchange persisted private messages, receiving near-real-time notification via SSE.
4. Authorization is enforced server-side; anonymous users cannot access private data, users cannot edit another user's data, and direct API requests cannot bypass these rules.
5. The same codebase runs locally and can be deployed manually on a small Alpine Linux server with NGINX TLS termination, Spring Boot as an OpenRC service and persistent embedded H2 storage.
6. Automated tests and a GitHub Actions CI pipeline verify a reproducible build; the target Alpine deployment is smoke-tested.

## 2. Decision ledger and boundaries

### Confirmed
- **Frontend:** Angular 22, standalone components, signals and feature-scoped services, RxJS for asynchronous data, Angular Material, reactive forms, English-only interface, WCAG 2.2 AA target.
- **PWA:** Installable frontend served by NGINX; application shell/assets cached for offline loading, with data interactions online-only and an update notification.
- **Backend:** OpenJDK 25, Spring Boot 4.1.2, modular monolith, feature-oriented packages, versioned `/api/v1` REST API, JSON, RFC 9457-style problem errors, OpenAPI/Swagger restricted on public hosting.
- **Data:** H2 file-backed, Flyway schema migrations, Spring Data JPA, Hibernate schema validation, SQL constraints, server-side pagination. One backend instance has exclusive ownership of its H2 database.
- **Infrastructure:** One small Alpine Linux host; NGINX static frontend and same-origin `/api` reverse proxy; TLS terminates at NGINX; backend binds privately as a non-root OpenRC service; no Docker, no separate PostgreSQL, no uploaded files.
- **Authentication:** Username/password only; registration is open, no email required; single normal-user permission model, **no admin**; secure HTTP-only session cookie, SameSite=Lax, seven-day inactivity timeout; CSRF protection; hashed passwords, rate-limited login and registration, password change while logged in; no automated password recovery.
- **Username:** Immutable, case-insensitively unique, **3–30 lowercase Latin letters (`a`–`z`) only**, with reserved names blocked. Public URL `/profile/{username}`.
- **Registration:** username, password, first and last name, professional headline and short summary mandatory so a public profile is never empty. Public profile appears immediately; unused optional sections are omitted.
- **Profiles:** Biography, experience, education, skills, languages, projects, certifications, links; section-by-section explicit save/cancel; fixed section order, entry ordering within sections. No profile photo uploads; generated initials-based avatar.
- **Social:** Public text-only chronological posts (plain text/line breaks/links), likes, flat comments, edit/delete owned posts/comments, public directory filtered by name/headline/skills; invitation-based mutual connections and accepted public connections; pending invitations private; chronological all-post and connection feeds.
- **Messaging:** Only accepted connections can send; one 1:1 conversation per pair; immutable plain-text message history, hide a conversation locally, read-state/unread counts; no groups, typing indicators, online presence, attachments or delivery badges. Send via REST and notify with **SSE**; persist before notification and recover missed events by re-fetching data after reconnect.
- **Notifications:** In-app connection and message activity, SSE updates, no email or browser push.
- **CV:** On-demand public PDF from structured profile; fixed ATS-friendly single-column multi-page layout; excludes posts/connections/messages.
- **Testing & release:** Unit, integration, API security, Angular component, and targeted E2E tests; GitHub Actions build/lint/test without auto-deploy; logs and restricted Actuator health; environment-driven config; documented consistent backup/restore and manual deployment/rollback; concise README with setup/test/deployment.
- **Seed data:** Fictional profiles, experiences, connections, posts and conversations initialized in a controlled development/demo-only workflow.

### Superseded (do NOT implement)
- Uploaded profile pictures or post images, external image storage.
- WebSocket chat; replace with REST + SSE.
- Separate PostgreSQL server; replace with H2 file database.
- Backend JAR serving Angular; Angular served by NGINX.
- Docker/Compose, systemd, multi-host/microservice deployments.
- Email-based sign-in, social login, administrator role, automated email recovery.
- Substantial architecture handbooks; provide concise operational README plus generated OpenAPI.

### Pending product defaults — proposed, **not explicitly approved**
The user did not respond to Round 8's Q116–Q137 individually. These are implementation-planning defaults, not settled decisions. Tickets tagged `PROPOSED` must be confirmed before implementation:
- Independent browser tabs and simultaneous device sessions (logout current session only); password change invalidates other sessions.
- Informational profile-completeness indicator; free-text case-insensitive distinct skills; multiple concurrent ongoing roles; validated HTTP/HTTPS links without preview fetching.
- Re-invitations after declining, invitation/acceptance notification rules, like toggles, simple directory substring matching.
- Hidden conversations reappear on incoming messages; conversations marked read when viewed; up to 4,000 characters/message; safe idempotent retry.
- Persisted notification center with mark read/all read; notifications retained until parent content/account removal.
- CV metadata/Unicode-capable fonts/clickable links; per-export section selection.
- Shareable query-string search/pagination, public-endpoint limits and rate limits, demo users with ordinary account permissions.
These defaults can be changed without reopening the settled platform architecture. Work on them should not start until approved.

## 3. Users, roles and core workflows

**Anonymous visitor:** Home/public feed → discover/search people → public profile → public connections / public posts → download PDF CV → register/login if interacting.

**Registered user:** Register with required identity fields → session established → dashboard and optional profile guidance → edit individual sections → create posts, like/comment → search people, request/accept/decline connections → see connection-specific feed → privately message accepted connections → read notifications → manage password or delete account.

**No administrator:** Moderation, job listings, recommendations, followers, endorsements, premium subscriptions, groups, media upload, email workflows and blocking/reporting are out of scope in this demo. A public launch beyond controlled demonstration would require revisiting abuse handling.

## 4. Technical architecture

```text
Browser (Angular 22 installable PWA)
  | HTTPS, same-origin / and /api/v1
  v
NGINX (Alpine Linux)
  |-- serves immutable hashed Angular assets and SPA fallback
  |-- TLS, redirects, security headers
  `-- /api/v1/* proxy over localhost -> Spring Boot 4.1.2 / Java 25
                                            |-- authentication + authorization
                                            |-- profiles/CV
                                            |-- feed/people/connections
                                            |-- conversations/notifications/SSE
                                            `-- Spring Data JPA + Flyway
                                                     `-- H2 file on persistent disk
```

- Angular calls relative `/api/v1` endpoints. Session cookie is HTTP-only and Secure; CSRF cookie/token is distinct and used by Angular's unsafe-method header integration.
- SSE uses a same-origin authenticated GET stream. A tab receives event identifiers/metadata only, then fetches authorized records using REST. Disable proxy buffering for SSE, configure heartbeat and idle timeout, verify NGINX settings.
- Spring Boot listens only on loopback. Honor forwarded headers only from trusted NGINX and never blindly trust public request headers.
- Use feature ownership with boundaries: `identity`, `profiles`, `social`, `connections`, `messaging`, `notifications`, `cv`, `shared` (small, cross-cutting only).
- Keep HTML untrusted: Angular template interpolation escapes user text; links validated to HTTP/HTTPS; backend validates input and authorization.
- Service worker must never cache personalized JSON, private messages, authenticated API responses or SSE streams. Cache application shell only.
- Site must work when user reopens browser online after an offline visit; show clear offline states for data operations.

### Suggested repository layout

```text
/
  frontend/                 # Angular app
  backend/                  # Spring Boot app
  deploy/alpine/            # NGINX/OpenRC config, scripts and example env
  docs/                     # concise supporting design artifacts if needed
  .github/workflows/        # CI
  README.md
```

## 5. Domain and persistence model

Core entities (exact columns to be finalized with tickets):

| Aggregate / entity | Key data | Rules |
|---|---|---|
| `UserAccount` | id, username, passwordHash, status, createdAt | username immutable, lowercase letters only, unique; no admin |
| `Profile` | userId, firstName, lastName, headline, summary, location, links | public, required identity at registration, ownership via userId |
| `Experience` | profileId, employer, title, type, location, start/end, current, description, ordinal | multiple current roles supported by schema |
| `Education` | profileId, institution, degree, field, dates, notes, ordinal | optional entries |
| `Certification` | profileId, issuer, name, issue/expiry, ordinal | optional expiry |
| `Project` | profileId, title, description, timespan, technologies, URL, ordinal | links validated |
| `Skill` / `ProfileSkill` | profileId, normalized label, ordinal | unique per profile by normalized name (proposed) |
| `Language` | profileId, name, proficiency, ordinal | configurable enum |
| `Post` | id, authorId, body, createdAt, updatedAt | public, text only, editable by author |
| `PostLike` | postId, userId | unique per pair; toggle |
| `Comment` | id, postId, authorId, body, timestamps | flat comments; owner edits/deletes |
| `ConnectionRequest` / `Connection` | requester, recipient, status, timestamps | pending direction, accepted undirected pair; no self/duplicate |
| `Conversation` | id, canonical participant pair, last activity | max one per pair |
| `Message` | id, conversationId, senderId, text, createdAt, clientRequestId | immutable; send requires accepted connection |
| `ConversationState` | conversationId, userId, readPointer, hidden | per-user read/hide behavior |
| `Notification` | id, recipientId, kind, actorId, target ref, readAt, createdAt | persist if approved Round 8 default |

**Relational invariants:** Foreign keys and indexes for owner and chronology, unique normalized usernames, canonical (minId,maxId) connection/conversation pairs, unique post-like pairs, constraints against self-connection. Avoid unbounded object graphs in JSON: use DTOs rather than exposing JPA entities. Transactions wrap connection changes, deletes, message persistence and notification creation. Specify deletion of shared conversation messages on account deletion explicitly: deleting a user deletes their participation/history as agreed, including shared history.

**Migrations:** Versioned Flyway SQL. Validate mapping at startup (`ddl-auto=validate`). Demo seed data loaded by a dedicated guarded command/config only in demo or development, idempotently; never through automatic production startup.

**Pagination:** Default page size 20, max 100 for lists; stable sort using timestamp plus ID. Chat history uses newest-30 window and older-history pagination. Page-based API agreed; message-history cursors are a practical implementation option requiring explicit confirmation if introduced.

## 6. Functional feature specifications

### Identity and profile
- Registration wizard/form captures required professional fields before account creation; errors map to fields. Existing username conflicts return clear messages; registration creates account and nonempty profile atomically.
- Public URL `/profile/{username}`; immutable lowercase-only username; generated initials avatar from display name; profile editor for each structured section with Save/Cancel and unsaved-change navigation guard.
- Entries have ordering within each section; no cross-section reorder. Date logic validates start before end and current entries without end date. Empty optional sections do not appear publicly.
- Account settings support authenticated password change, logout, and irreversible deletion with confirmation; no recovery email.

### Feed, directory, and network
- Feed is public and ordered newest-first, with a filtered view for posts by accepted connections for logged-in users. Empty/loaded/error/retry states.
- Posts are plain text, line breaks and safe external hyperlinks; likes toggle per user; flat comments, no nested replies or attachments.
- People directory searches public name/headline/skill fields and returns bounded paginated results. Public accepted connection lists are visible; pending requests are private.
- Connection request state machine: `PENDING -> ACCEPTED / DECLINED / CANCELLED`; accepted connection can be removed. Acceptance only by recipient, cancellation only by requester. Sending a new message requires a current accepted connection.

### Messaging and notifications
- At most one private conversation per unordered pair of users; ordered messages, no edits. Create/send by REST and notify relevant authorized SSE subscribers after commit.
- Existing history remains viewable after disconnect; no new sends until reconnect. Hidden conversation is a per-user presentation state; no deletion for other participant.
- Unread states are per recipient; conversation list sorted by last activity; newest messages loaded first then older messages incrementally.
- SSE emits event metadata, not full message bodies. Reconnection triggers authoritative REST refresh, handles missed events, and uses bounded retry/backoff. No service-worker caching for stream/private APIs.
- Notifications: connection invitations, accepted invitations, unread-message counts; UI in-app only, no push or email.

### CV export
- Public endpoint returns PDF for a public username, created on demand from current profile fields. Stable single-column ATS-friendly layout, multiple pages, reasonable typography and meaningful headings.
- No social activity or private data in PDF. Return appropriate content type, filename and cache headers; evaluate personally identifying information fields explicitly (public-profile contract).

## 7. API outline (subject to ticket-level detailed contracts)

| Domain | Representative endpoints |
|---|---|
| Session | `POST /api/v1/auth/register`, `/login`, `/logout`; `GET /api/v1/auth/me`; `POST /api/v1/auth/password` |
| Profiles | `GET /api/v1/profiles/{username}`; `PATCH /api/v1/me/profile`; CRUD under `/api/v1/me/experiences`, `/educations`, `/projects`, `/skills`, `/certifications`, `/languages` |
| People | `GET /api/v1/people?query=...&skill=...&page=...` |
| Feed | `GET /api/v1/posts`, `/feed/connections`; `POST /api/v1/posts`; `PATCH/DELETE /api/v1/posts/{id}` |
| Engagement | `PUT/DELETE /api/v1/posts/{id}/like`; `GET/POST /api/v1/posts/{id}/comments`; owner comment edits/deletes |
| Network | `GET/POST /api/v1/connections/requests`; accept/decline/cancel endpoints; `DELETE /api/v1/connections/{userId}` |
| Messages | `GET /api/v1/conversations`; `GET /api/v1/conversations/{id}/messages`; `POST .../messages`; mark-read/hide operations |
| Notifications | `GET /api/v1/notifications`; mark read and unread count |
| Events | `GET /api/v1/events` authenticated SSE |
| CV | `GET /api/v1/profiles/{username}/cv` |

Use standards-oriented RFC 9457 error objects with stable application error codes, trace ID and a field error extension. Use 400 for bad input, 401 unauthenticated, 403 forbidden, 404 not-found, 409 state conflict, 429 rate limit. Avoid leaking existence of private resources when unauthorized.

## 8. Nonfunctional requirements

**Security:** Password hashing using an adaptive encoder (e.g. Argon2id if dependency/operational fit validated; otherwise Spring-supported BCrypt); 12+ characters. Secure session cookie, SameSite=Lax, CSRF integration tested on login/logout and state-changing requests. Session max idle seven days; revoke server-side on logout. Login/register throttling; limits on public searches/requests. No secrets in git, logs, or demo content. Restrict Actuator and interactive API docs.

**Performance:** Target reliable interaction with several concurrently browsing demo users on a small Alpine host, not enterprise throughput. Bound all collection responses; watch H2 connection pool, memory and SSE emitters. Establish quantitative baseline from smoke/load measurement rather than inventing SLA numbers.

**Observability:** Actuator health protected as appropriate, structured logs with correlation IDs, safe redaction, useful error handling, OpenRC service health check. Collect disk headroom metrics manually or via lightweight monitoring.

**Data safety:** H2 `BACKUP TO` or equivalent transactionally consistent online backup; store compressed backups separately from the live DB. Backup procedures cover application data (no uploaded media). Restore drill against nonproduction copy; migration backups before releases and rollback procedures for database changes.

**Accessibility:** Keyboard navigation, visible focus, labels, aria-live alerts, usable color contrast, reduced-motion-friendly indicators and meaningful error announcements. Check automated + manual keyboard tests.

**PWA:** Installable manifest/icons, app-shell-only caching and update prompts. Private `/api/v1` requests must bypass offline cache; test logout and change-of-user to prevent private data exposure via browser cache.

## 9. Delivery roadmap and gates

| Milestone | Outcome | Tickets | Exit gate |
|---|---|---|---|
| M0 — Foundation | Repo, apps, CI, conventions | PN-001–PN-007 | Both apps build, CI passes, startup documented |
| M1 — Secure identity | H2 schema, auth, registration, sessions, profile bootstrap | PN-008–PN-016 | Registration, login/logout, access-control tests pass |
| M2 — Professional profile and CV | Structured profiles, public pages, CV | PN-017–PN-026 | Nonempty profiles, ownership tests, usable PDF |
| M3 — Social graph and feed | Posts, likes, comments, people search, connections | PN-027–PN-037 | Multi-user social workflows pass E2E |
| M4 — Messaging and notifications | Conversation model, REST, SSE, UI and recovery | PN-038–PN-048 | Two users reliably exchange/recover messages |
| M5 — PWA & hardening | Installability, security, accessibility, e2e | PN-049–PN-056 | Test/safety quality gates pass |
| M6 — Alpine release | NGINX, OpenRC, backup, deployment, smoke test | PN-057–PN-063 | Clean Alpine deployment and restore verified |

Sequence can overlap where dependencies allow; no fixed calendar or effort estimates have been approved. `tickets.csv` contains a suggested relative sizing in points for planning only, not a committed schedule.

## 10. Quality gates and Definition of Done

Each ticket: requirements and acceptance criteria satisfied, unit/integration tests for changed behavior, security/privacy test cases where applicable, accessible handling of loading/errors, API docs for new endpoints, clean CI, reviewed migration impact. No new dependency without a reason.

Final demo acceptance scenarios:
1. Anonymous visitor sees only public information, searches for a user, opens profile and downloads valid multipage CV.
2. New registration requires meaningful profile fields; invalid/duplicate username is rejected, and username cannot be changed.
3. User A cannot change User B's profile, post, comment, connection request, conversation or notifications through direct API calls.
4. A posts; B likes/comments; A edits/deletes own post and comments/likes are removed consistently.
5. A invites B; B accepts; both can message; C cannot see their conversation; after disconnect, A and B can read history but cannot send.
6. Simulate SSE disconnect; state recovers via REST after reconnect without losing persisted messages.
7. Session cookies and CSRF checks function behind real NGINX TLS; restricted endpoints are not publicly exposed.
8. Service worker loads offline shell without returning private API content from cache; update notification works.
9. Flyway migration, backup/restore, and one-instance H2 invariants are verified; deployment and rollback scripts work on target Alpine.
10. Automated CI and representative browser/security accessibility checks are green.

## 11. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Alpine / Java 25 runtime footprint and compatibility | Early Alpine smoke spike; validate JVM distribution, memory, startup, fonts and native libraries before feature expansion |
| H2 file corruption or accidental concurrent opens | Single backend owner, controlled service stop, consistent backups, restore test, migration backup |
| PWA exposes stale/private content | Cache shell only, exclude `/api` and SSE from caches, test login/logout/account switches |
| NGINX buffers SSE and makes chat appear delayed | Disable proxy buffering for SSE, ensure flush/heartbeat, test real NGINX behavior |
| Session/CSRF mismatch through proxy | Automated end-to-end login/logout/form submissions against HTTPS environment |
| No moderator/blocking/reporting in a publicly accessible network | Limit to controlled demo/fictional data, request throttling, abuse and availability protections; reassess before general public adoption |
| Deletion changes shared conversation history | Explicit user-facing deletion warning and transactional cascade tests |
| Browser tabs are not synchronized | Treat Q116 alternative as pending; handle out-of-date tabs with REST refresh and safe conflict errors |
| Generic CV fails font/Unicode/page breaking | Validate embedded fonts and real sample content including long text; inspect PDFs |

## 12. Open approvals / change control

- **Round 8 Q116–Q137 were not answered individually.** They are not silently accepted; the linked backlog rows are marked `PROPOSED`, and those defaults require approval before implementation.
- Detailed field limits (other than accepted general examples), exact message-retention behavior for shared deletion, SSE event versioning, API path refinements and pagination serialization will be finalized within corresponding tickets, with a decision record if they change product semantics.
- Creation of this project plan and backlog is not authorization to create source code, provision infrastructure, create GitHub issues, or begin deployment.

## 13. Reference documentation

- Angular release policy: https://angular.dev/reference/releases
- Angular service worker: https://angular.dev/ecosystem/service-workers
- Angular updates: https://angular.dev/ecosystem/service-workers/communications
- Spring Boot 4.1.2 reference: https://docs.spring.io/spring-boot/
- Spring Security CSRF integration: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
