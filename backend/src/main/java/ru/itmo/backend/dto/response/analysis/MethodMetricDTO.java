package ru.itmo.backend.dto.response.analysis;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a single metric value for a method")
public record MethodMetricDTO(
        @Schema(description = "Unique metric identifier")
        String metricCode,
        @Schema(description = "Metric value")
        Double value
) {}

