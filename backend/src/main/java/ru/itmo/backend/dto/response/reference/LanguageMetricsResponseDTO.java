package ru.itmo.backend.dto.response.reference;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Represents a set of metrics supported for a specific programming language")
public record LanguageMetricsResponseDTO(
        @Schema(description = "List of available metrics for the language")
        List<MetricDTO> items
) {}

