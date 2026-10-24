package com.vynk.shared.error;

import java.util.Map;

/**
 * Base exception for expected API failures that should become RFC 9457 problem responses.
 *
 * <p>Feature code should throw these instead of returning ad-hoc error payloads or letting
 * low-level exceptions escape. The {@link GlobalExceptionHandler} owns the HTTP mapping.</p>
 */
public class ApiException extends RuntimeException {

    private final ApiErrorCode code;
    private final Map<String, Object> properties;

    public ApiException(ApiErrorCode code, String detail) {
        this(code, detail, Map.of(), null);
    }

    public ApiException(ApiErrorCode code, String detail, Map<String, Object> properties) {
        this(code, detail, properties, null);
    }

    public ApiException(ApiErrorCode code, String detail, Throwable cause) {
        this(code, detail, Map.of(), cause);
    }

    private ApiException(ApiErrorCode code, String detail, Map<String, Object> properties, Throwable cause) {
        super(detail, cause);
        this.code = code;
        this.properties = Map.copyOf(properties);
    }

    public ApiErrorCode code() {
        return code;
    }

    /**
     * Additional RFC 9457 extension members to include in the problem response.
     */
    public Map<String, Object> properties() {
        return properties;
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(ApiErrorCode.BAD_REQUEST, detail);
    }

    public static ApiException validationFailed(String detail) {
        return new ApiException(ApiErrorCode.VALIDATION_FAILED, detail);
    }

    public static ApiException unauthenticated(String detail) {
        return new ApiException(ApiErrorCode.UNAUTHENTICATED, detail);
    }

    public static ApiException forbidden(String detail) {
        return new ApiException(ApiErrorCode.FORBIDDEN, detail);
    }

    /**
     * Creates a not-found failure. Use this for private resources too, so unauthorized callers
     * cannot infer that a record exists.
     */
    public static ApiException notFound(String detail) {
        return new ApiException(ApiErrorCode.NOT_FOUND, detail);
    }

    public static ApiException conflict(String detail) {
        return new ApiException(ApiErrorCode.CONFLICT, detail);
    }

    public static ApiException rateLimited(String detail) {
        return new ApiException(ApiErrorCode.RATE_LIMITED, detail);
    }
}
