package com.vynk.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Bounded, zero-based page request shared by every list endpoint.
 *
 * <p>Controllers bind this with {@code @Valid @ModelAttribute PageQuery pageQuery}. Omitted
 * parameters fall back to {@link #DEFAULT_PAGE} and {@link #DEFAULT_SIZE}; explicit values outside
 * the allowed range are rejected with a validation problem response rather than silently
 * clamped.</p>
 *
 * @param page zero-based page index, or {@code null} when the client omitted it
 * @param size requested page size, or {@code null} when the client omitted it
 */
@Schema(description = "Zero-based page request shared by all list endpoints.")
public record PageQuery(
        @Schema(description = "Zero-based page index.", defaultValue = "0", minimum = "0")
        @Min(value = 0, message = "Page index must be zero or greater.")
        Integer page,

        @Schema(
                description = "Page size. Maximum is 100.",
                defaultValue = "20",
                minimum = "1",
                maximum = "100")
        @Min(value = 1, message = "Page size must be at least 1.")
        @Max(value = PageQuery.MAX_SIZE, message = "Page size must not exceed 100.")
        Integer size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    /**
     * @return the requested page index, or {@link #DEFAULT_PAGE} when omitted
     */
    public int pageNumber() {
        return page == null ? DEFAULT_PAGE : page;
    }

    /**
     * @return the requested page size, or {@link #DEFAULT_SIZE} when omitted
     */
    public int pageSize() {
        return size == null ? DEFAULT_SIZE : size;
    }
}
