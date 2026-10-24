# API conventions

## Versioning
- All API endpoints live under `/api/v1`.
- Do not create unversioned production endpoints.
- Use `ApiPaths.BASE` (`/api/v1`) in controllers.

## Errors
- All expected and unexpected failures use RFC 9457 Problem Details responses (`application/problem+json`).
- Every problem response includes `type`, `title`, `status`, `code` and `traceId`. The `X-Trace-Id` header is always returned.
- `code` values are stable members of `ApiErrorCode`. Never rename enum constants.
- Throw `ApiException` for deliberate failures; `GlobalExceptionHandler` owns HTTP mapping.
- For private resources, return `404 Not Found` (or a privacy-preserving response) rather than `403` to avoid leaking existence.
- Validation failures include an `errors` array of `FieldViolation` objects (`field`, `message`).
- Unexpected failures are logged server-side with stack traces but never expose exception details to clients.

## Pagination
- List endpoints accept `page` (zero-based, default 0) and `size` (default 20, max 100) via `PageQuery`.
- Responses use `PageResponse<T>` with `items`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`.
- Ordering must be documented and stable (typically newest-first with a deterministic ID tie-breaker).

## Authentication and authorization
- Session cookie is HTTP-only, Secure, SameSite=Lax; CSRF protection is required for unsafe methods.
- Unauthorized requests to protected endpoints result in `401 UNAUTHENTICATED`.
- Forbidden actions result in `403 FORBIDDEN` when existence must not be leaked; otherwise use appropriate privacy-preserving responses as specified by the ticket.
- Logs and responses must never contain sensitive information or secrets.

## OpenAPI
- The OpenAPI document is generated locally at `/v3/api-docs`. Swagger UI is disabled by default.
- Set `VYNK_API_DOCS_ENABLED=false` in deployed environments. Never route `/v3/api-docs` through public NGINX.
- Shared schemas (`Problem`, `FieldViolation`, `PageResponse`) are published in the document for client generation.
