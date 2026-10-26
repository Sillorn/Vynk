package com.vynk.shared.error;

import java.net.URI;
import org.springframework.http.HttpStatus;

/**
 * Stable application error codes exposed to API clients.
 *
 * <p>The enum constant name is the public {@code code} member of every problem response and must
 * not be renamed without an API contract change. Each constant also provides the RFC 9457 problem
 * {@code type} URI and a human-readable default title.</p>
 */
public enum ApiErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad Request", "bad-request"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation Failed", "validation-failed"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication Required", "unauthenticated"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden", "forbidden"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found", "not-found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", "method-not-allowed"),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "Not Acceptable", "not-acceptable"),
    CONFLICT(HttpStatus.CONFLICT, "Conflict", "conflict"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", "unsupported-media-type"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", "rate-limited"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "internal-error");

    private final HttpStatus status;
    private final String title;
    private final URI type;

    ApiErrorCode(HttpStatus status, String title, String typeSlug) {
        this.status = status;
        this.title = title;
        this.type = URI.create("urn:vynk:error:" + typeSlug);
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public URI type() {
        return type;
    }
}
