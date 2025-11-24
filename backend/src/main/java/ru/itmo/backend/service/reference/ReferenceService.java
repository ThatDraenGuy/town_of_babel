package ru.itmo.backend.service.reference;

import org.springframework.stereotype.Service;
import ru.itmo.backend.dto.LanguageMetricsDTO;
import ru.itmo.backend.dto.MetricDTO;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReferenceService {

    private final ReferenceProperties referenceProperties;

    public ReferenceService(ReferenceProperties referenceProperties)
    {
        this.referenceProperties = referenceProperties;
    }

    public List<LanguageMetricsDTO> getReference()
    {
        return referenceProperties.getLanguages().stream()
                .map(lang -> new LanguageMetricsDTO(
                        lang.getLanguage(),
                        lang.getMetrics().stream()
                                .map(m -> new MetricDTO(m.getId(), m.getName(), m.getDescription()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}