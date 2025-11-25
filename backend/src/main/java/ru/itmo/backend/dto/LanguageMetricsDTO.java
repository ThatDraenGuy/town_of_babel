package ru.itmo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Represents a set of metrics supported for a specific programming language")
public record LanguageMetricsDTO(

        @Schema(description = "Programming language name", example = "Java")
        String language,

        @Schema(description = "List of available metrics for the language")
        List<MetricDTO> metrics
) {}
