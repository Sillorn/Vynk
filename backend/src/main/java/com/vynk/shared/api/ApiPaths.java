package com.vynk.shared.api;

/**
 * Public API path constants.
 *
 * <p>All endpoints live under a single versioned base path. Bump the version only through an API
 * contract change; never add unversioned production endpoints.</p>
 */
public final class ApiPaths {

    /**
     * Base path for the current API version.
     */
    public static final String BASE = "/api/v1";

    private ApiPaths() {
    }
}
