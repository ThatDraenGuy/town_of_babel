package ru.itmo.babel.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
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

    public static class LanguageConfig
    {
        private String language;
        private List<MetricConfig> metrics;

        public String getLanguage()
        {
            return language;
        }

        public void setLanguage(String language)
        {
            this.language = language;
        }

        public List<MetricConfig> getMetrics()
        {
            return metrics;
        }

        public void setMetrics(List<MetricConfig> metrics)
        {
            this.metrics = metrics;
        }
    }

    public static class MetricConfig {
        private String id;
        private String name;
        private String description;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

       public void setName(String name)
       {
            this.name = name;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }
}
