package ru.itmo.backend.evaluator.model;

import java.util.HashMap;
import java.util.Map;

public record MethodMetric(String name, Map<String, String> own) {
    MethodMetric(String name) {
        this(name, new HashMap<>());
    }
}
