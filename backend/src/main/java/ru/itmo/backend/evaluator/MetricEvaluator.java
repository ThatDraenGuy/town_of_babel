package ru.itmo.backend.evaluator;

import ru.itmo.backend.evaluator.model.ClassMetric;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public interface MetricEvaluator {
    public Map<String, ClassMetric> evaluateMetrics(File repository, Predicate<Path> filesFilter, List<String> metrics) throws IOException, MetricEvaluationException;
}
