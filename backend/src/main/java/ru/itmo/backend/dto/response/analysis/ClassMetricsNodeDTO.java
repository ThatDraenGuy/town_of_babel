package ru.itmo.backend.dto.response.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Represents metrics for a class")
public record ClassMetricsNodeDTO(
        @Schema(description = "Class name")
        String name,
        @Schema(description = "List of method metrics for this class")
        List<MethodMetricsNodeDTO> items
) implements MetricsNodeDTO {
    @Override
    public String getName() {
        return name;
    }
}

