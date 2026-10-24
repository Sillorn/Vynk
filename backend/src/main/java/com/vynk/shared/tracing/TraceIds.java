package com.vynk.shared.tracing;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

/**
 * Standard names and helpers for the per-request trace ID.
 *
 * <p>The trace ID is returned in the {@value #HEADER} response header, stored as a request
 * attribute for server-side diagnostics, and placed in the MDC so log lines can be correlated.</p>
 */
public final class TraceIds {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";
    public static final String REQUEST_ATTRIBUTE = TraceIds.class.getName() + ".traceId";

    private static final Pattern ACCEPTED_CLIENT_VALUE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{7,127}");

    private TraceIds() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Accepts a caller-supplied trace ID only when it is short, bounded and header-safe.
     */
    public static Optional<String> acceptedClientValue(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }
        String candidate = rawValue.trim();
        return ACCEPTED_CLIENT_VALUE.matcher(candidate).matches() ? Optional.of(candidate) : Optional.empty();
    }

    /**
     * Returns the trace ID for the current thread, if a request is being handled.
     */
    public static String current() {
        String value = MDC.get(MDC_KEY);
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * Returns the request's trace ID, creating and attaching one when the filter did not run.
     */
    public static String ensure(HttpServletRequest request) {
        Object attribute = request.getAttribute(REQUEST_ATTRIBUTE);
        if (attribute instanceof String value && ACCEPTED_CLIENT_VALUE.matcher(value).matches()) {
            return value;
        }
        String traceId = current();
        if (traceId == null) {
            traceId = generate();
        }
        request.setAttribute(REQUEST_ATTRIBUTE, traceId);
        return traceId;
    }
}
