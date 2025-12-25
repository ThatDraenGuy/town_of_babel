package ru.itmo.backend.dto.response.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.itmo.backend.dto.response.commit.CommitDTO;

@Schema(description = "Represents metrics for a specific commit")
public record CommitMetricsDTO(
        @Schema(description = "Commit information")
        CommitDTO commit,
        @Schema(description = "Root metrics node (usually a package)")
        MetricsNodeDTO root
) {}

