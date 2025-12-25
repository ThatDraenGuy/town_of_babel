package ru.itmo.backend.dto.response.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Represents metrics for a package")
public record PackageMetricsNodeDTO(
        @Schema(description = "Package name")
        String name,
        @Schema(description = "List of child nodes (packages or classes)")
        List<MetricsNodeDTO> items
) implements MetricsNodeDTO {
    @Override
    public String getName() {
        return name;
    }
}

