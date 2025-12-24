package ru.itmo.backend.dto.response.reference;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a single static analysis metric definition")
public record MetricDTO(

        @Schema(description = "Unique metric identifier", example = "cyclomatic_complexity")
        String metricCode,

        @Schema(description = "Human-readable metric name", example = "Cyclomatic Complexity")
        String metricName,

        @Schema(description = "Detailed description of what the metric measures",
                example = "Shows complexity of functions based on branching")
        String metricDescription,

        @Schema(description = "Type of the metric")
        MetricType metricType
) {
    public enum MetricType {
        NUMERIC,
        COLOR
    }
}
