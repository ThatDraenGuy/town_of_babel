package ru.itmo.babel.dto;

import java.util.List;

public record LanguageMetricsDTO (String language, List<MetricDTO> metrics) {}
