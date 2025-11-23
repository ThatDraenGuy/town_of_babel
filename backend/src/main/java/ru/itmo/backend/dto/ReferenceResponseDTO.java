package ru.itmo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response containing aggregated language and metric statistics")
public record ReferenceResponseDTO(

        @Schema(description = "List of recognized programming languages with metrics")
        List<LanguageMetricsDTO> languages,

        @Schema(description = "List of general analysis metrics")
        List<MetricDTO> metrics

) {}
