package ru.itmo.backend.evaluator.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.itmo.backend.evaluator.MetricEvaluationException;

import java.util.HashMap;
import java.util.Map;

public record ClassMetric(String name, Map<String, String> own, Map<String, MethodMetric> methods) {
    public ClassMetric(String name) {
        this(name, new HashMap<>(), new HashMap<>());
    }

    public String toJson() throws MetricEvaluationException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw  new MetricEvaluationException("Results serialization failed", e);

        }
    }
}
