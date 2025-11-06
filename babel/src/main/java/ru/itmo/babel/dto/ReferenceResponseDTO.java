package ru.itmo.babel.dto;

import java.util.List;

public record ReferenceResponseDTO(List<LanguageMetricsDTO> languages, List<MetricDTO> metrics) {}
