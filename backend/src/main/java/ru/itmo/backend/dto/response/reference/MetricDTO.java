package ru.itmo.backend.dto.response.reference;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a single static analysis metric definition")
public record MetricDTO(

        @Schema(description = "Unique metric identifier", example = "cyclomatic_complexity")
        String id,

        @Schema(description = "Human-readable metric name", example = "Cyclomatic Complexity")
        String name,

        @Schema(description = "Detailed description of what the metric measures",
                example = "Shows complexity of functions based on branching")
        String description
) {}
