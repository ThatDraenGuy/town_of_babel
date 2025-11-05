package ru.itmo.babel.dto;

import java.util.List;

public class LanguageMetricsDTO {
    private String language;
    private List<MetricDTO> metrics;

    public LanguageMetricsDTO()
    {
    }

    public LanguageMetricsDTO(String language, List<MetricDTO> metrics)
    {
        this.language = language;
        this.metrics = metrics;
    }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public List<MetricDTO> getMetrics() { return metrics; }
    public void setMetrics(List<MetricDTO> metrics) { this.metrics = metrics; }
}
