package ru.itmo.backend.service.reference;

import org.springframework.stereotype.Service;
import ru.itmo.backend.dto.response.reference.LanguageMetricsResponseDTO;
import ru.itmo.backend.dto.response.reference.LanguageResponseDTO;
import ru.itmo.backend.dto.response.reference.MetricDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReferenceService {

    private final ReferenceProperties referenceProperties;

    public ReferenceService(ReferenceProperties referenceProperties)
    {
        this.referenceProperties = referenceProperties;
    }

    public List<LanguageResponseDTO> getLanguages() {
        List<LanguageResponseDTO> result = new ArrayList<>();
        List<ReferenceProperties.LanguageConfig> configs = referenceProperties.getLanguages();
        for (int i = 0; i < configs.size(); i++) {
            String name = configs.get(i).getLanguage();
            result.add(new LanguageResponseDTO((long) (i + 1), name.toLowerCase(), name));
        }
        return result;
    }

    public Optional<LanguageMetricsResponseDTO> getLanguageMetrics(String languageCode) {
        return referenceProperties.getLanguages().stream()
                .filter(lang -> lang.getLanguage().equalsIgnoreCase(languageCode))
                .findFirst()
                .map(lang -> new LanguageMetricsResponseDTO(
                        lang.getMetrics().stream()
                                .map(m -> new MetricDTO(
                                        m.getId(),
                                        m.getName(),
                                        m.getDescription(),
                                        MetricDTO.MetricType.NUMERIC // Default to NUMERIC
                                ))
                                .collect(Collectors.toList())
                ));
    }
}