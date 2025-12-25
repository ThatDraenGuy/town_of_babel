package ru.itmo.backend.service.reference;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import ru.itmo.backend.dto.response.reference.MetricDTO;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "reference")
public class ReferenceProperties {

    private List<LanguageConfig> languages;

    public List<LanguageConfig> getLanguages()
    {
        return languages;
    }

    public void setLanguages(List<LanguageConfig> languages)
    {
        this.languages = languages;
    }

    @Data
    public static class LanguageConfig
    {
        private String language;
        private List<MetricConfig> metrics;
    }

    @Data
    public static class MetricConfig {
        private String id;
        private String name;
        private String description;
        private MetricDTO.MetricType type;
    }
}