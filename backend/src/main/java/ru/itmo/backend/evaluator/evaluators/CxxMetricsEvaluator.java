package ru.itmo.backend.evaluator.evaluators;

import ru.itmo.backend.evaluator.lizard.LizardRunner;
import ru.itmo.backend.evaluator.MetricEvaluationException;
import ru.itmo.backend.evaluator.MetricEvaluator;
import ru.itmo.backend.evaluator.model.ClassMetric;
import ru.itmo.backend.evaluator.utils.FilesUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class CxxMetricsEvaluator implements MetricEvaluator {
    private static final List<String> CXX_EXTENSIONS = Arrays.asList(".cpp", ".cc", ".cxx", ".c++", ".C", ".h", ".hpp", ".hh", ".hxx", ".h++");

    @Override
    public Map<String, ClassMetric> evaluateMetrics(File repository, Predicate<Path> filesFilter, List<String> metrics) throws IOException, MetricEvaluationException {
        Predicate<Path> filter = FilesUtils.haveExtension(CXX_EXTENSIONS).and(filesFilter);
        List<Path> files = FilesUtils.collectPaths(repository, filter);
        var methods = LizardRunner.runLizard("cpp", files);
        return null;
    }
}
