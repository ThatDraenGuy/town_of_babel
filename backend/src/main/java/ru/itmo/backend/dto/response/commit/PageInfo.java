package ru.itmo.backend.dto.response.commit;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pagination information")
public record PageInfo(
        @Schema(description = "Current page index (0-based)")
        int page,
        @Schema(description = "Number of items per page")
        int pageSize,
        @Schema(description = "Total number of items available")
        long total
) {}

