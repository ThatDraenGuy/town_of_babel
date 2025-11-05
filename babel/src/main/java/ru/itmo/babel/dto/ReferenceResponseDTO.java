package ru.itmo.babel.dto;

import java.util.List;

public class ReferenceResponseDTO {
    private List<LanguageMetricsDTO> languages;
    private List<MetricDTO> metrics;

    public ReferenceResponseDTO() {}

    public ReferenceResponseDTO(List<LanguageMetricsDTO> languages, List<MetricDTO> metrics)
    {
        this.languages = languages;
        this.metrics = metrics;
    }

    public List<LanguageMetricsDTO> getLanguages() { return languages; }
    public void setLanguages(List<LanguageMetricsDTO> languages) { this.languages = languages; }

    public List<MetricDTO> getMetrics() { return metrics; }
    public void setMetrics(List<MetricDTO> metrics) { this.metrics = metrics; }
}
