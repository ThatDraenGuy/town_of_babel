package ru.itmo.backend.dto;

import java.util.List;

public record LanguageMetricsDTO (String language, List<MetricDTO> metrics) {}
