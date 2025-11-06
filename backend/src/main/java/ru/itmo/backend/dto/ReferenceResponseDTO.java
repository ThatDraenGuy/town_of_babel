package ru.itmo.backend.dto;

import java.util.List;

public record ReferenceResponseDTO(List<LanguageMetricsDTO> languages, List<MetricDTO> metrics) {}
