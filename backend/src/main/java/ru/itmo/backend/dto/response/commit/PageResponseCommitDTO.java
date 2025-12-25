package ru.itmo.backend.dto.response.commit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated response containing commits")
public record PageResponseCommitDTO(
        @Schema(description = "Current page index (0-based)")
        int page,
        @Schema(description = "Number of items per page")
        int pageSize,
        @Schema(description = "Total number of items available")
        long total,
        @Schema(description = "List of commits")
        List<CommitDTO> items
) {}

