package ru.itmo.backend.dto.response.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Represents metrics for a method")
public record MethodMetricsNodeDTO(
        @Schema(description = "Method name")
        String name,
        @Schema(description = "List of metrics for this method")
        List<MethodMetricDTO> metrics
) {}

