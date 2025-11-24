package ru.itmo.backend.dto.response;

import java.util.List;

/**
 * Generic paginated response.
 *
 * @param <T> element type
 * @param items page items
 * @param page current page index (0-based)
 * @param pageSize number of items per page
 * @param total total number of items available
 */
public record PageResponse<T>(List<T> items, int page, int pageSize, long total) {

    /**
     * Convenience factory for an empty page.
     */
    public static <T> PageResponse<T> empty(int page, int pageSize) {
        return new PageResponse<>(List.of(), page, pageSize, 0L);
    }
}
