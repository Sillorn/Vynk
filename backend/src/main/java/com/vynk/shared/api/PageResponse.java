package com.vynk.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Bounded page envelope returned by every list endpoint.
 *
 * <p>The envelope keeps page metadata explicit and stable across endpoints instead of relying on
 * framework-specific serialization. Ordering is always defined by the endpoint (typically newest
 * first with a deterministic id tie-breaker) so paging does not skip or repeat rows.</p>
 *
 * @param items the page content in the endpoint's documented order
 * @param page zero-based index of this page
 * @param size maximum number of items per page
 * @param totalElements total number of matching items across all pages
 * @param totalPages total number of pages for the current size
 * @param hasNext whether a page after this one exists
 * @param <T> item type
 */
@Schema(description = "Bounded page of results with navigation metadata.")
public record PageResponse<T>(
        @Schema(description = "Items in the endpoint's documented order.") List<T> items,
        @Schema(description = "Zero-based index of this page.", minimum = "0") int page,
        @Schema(description = "Maximum number of items per page.", minimum = "1", maximum = "100") int size,
        @Schema(description = "Total number of matching items.", minimum = "0") long totalElements,
        @Schema(description = "Total number of pages.", minimum = "0") int totalPages,
        @Schema(description = "Whether a page after this one exists.") boolean hasNext) {

    public PageResponse {
        items = items == null ? List.of() : List.copyOf(items);
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > PageQuery.MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + PageQuery.MAX_SIZE);
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
    }

    /**
     * Builds the envelope, deriving {@code totalPages} and {@code hasNext} from the totals.
     *
     * @param items the page content
     * @param page zero-based index of this page
     * @param size maximum number of items per page
     * @param totalElements total number of matching items across all pages
     * @param <T> item type
     * @return a complete page envelope
     */
    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalElements) {
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(items, page, size, totalElements, totalPages, page + 1 < totalPages);
    }
}
