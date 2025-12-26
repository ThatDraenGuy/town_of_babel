package ru.itmo.backend.evaluator.evaluators;

import ru.itmo.backend.evaluator.MetricEvaluationException;
import ru.itmo.backend.evaluator.MetricEvaluator;
import ru.itmo.backend.evaluator.lizard.LizardRunner;
import ru.itmo.backend.evaluator.model.ClassMetric;
import ru.itmo.backend.evaluator.model.MethodMetric;
import ru.itmo.backend.evaluator.utils.FilesUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class CSharpMetricsEvaluator implements MetricEvaluator {

    private static final List<String> CSHARP_EXTENSIONS = Arrays.asList(".cs", ".csx");


    private ClassMethod parseMethodName(String methodName, Map<String, String> metrics) throws MetricEvaluationException {
        String[] parts = methodName.split("::");
        String simpleMethodName = parts[parts.length - 1];
        String head = parts[0];
        String packageName = "";
        String className = head;
        if (head.contains(".")) {
            int lastDotIndex = head.lastIndexOf('.');
            packageName = head.substring(0, lastDotIndex);
            className = head.substring(lastDotIndex + 1);
        }
        return new ClassMethod(packageName, className, className, simpleMethodName);
    }

    Map<String, ClassMetric> lizardOutputToCSharpMetrics(Map<String, Map<String, String>> methods) throws MetricEvaluationException {
        Map<String, ClassMetric> result = new HashMap<>();

        for (var entry : methods.entrySet()) {
            ClassMethod raw = parseMethodName(entry.getKey(), entry.getValue());


            if (!result.containsKey(raw.className)) {
                ClassMetric classMetric = new ClassMetric(raw.className);
                classMetric.own().put("PACKAGE_NAME", raw.packageName);
                classMetric.own().put("SIMPLE_NAME", raw.simpleClassName);
                result.put(raw.className, classMetric);
            }

            ClassMetric classMetric = result.get(raw.className);
            classMetric.methods().put(raw.methodName, new MethodMetric(raw.methodName, LizardRunner.LizardFields.filterMetrics(entry.getValue())));
            result.put(raw.className, classMetric);
        }

        return result;
    }


    @Override
    public Map<String, ClassMetric> evaluateMetrics(File repository, Predicate<Path> filesFilter, List<String> metrics, MetricEvaluationContext context) throws IOException, MetricEvaluationException {
        Predicate<Path> filter = FilesUtils.haveExtension(CSHARP_EXTENSIONS).and(filesFilter);
        List<Path> files = FilesUtils.collectPaths(repository, filter);
        var lizardOutput = LizardRunner.runLizard("csharp", files);

        if (context.repoUrl() != null) {
            LizardRunner.generateGithubLocations(lizardOutput, context.repoUrl());
        }

        return lizardOutputToCSharpMetrics(lizardOutput);

    }

    private record ClassMethod(String packageName, String className,
                               String simpleClassName, String methodName) {
    }
}
