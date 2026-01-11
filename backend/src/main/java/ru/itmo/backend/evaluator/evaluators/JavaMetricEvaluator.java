package ru.itmo.backend.evaluator.evaluators;

import ru.itmo.backend.evaluator.lizard.LizardRunner;
import ru.itmo.backend.evaluator.MetricEvaluationException;
import ru.itmo.backend.evaluator.MetricEvaluator;
import ru.itmo.backend.evaluator.model.ClassMetric;
import ru.itmo.backend.evaluator.model.MethodMetric;
import ru.itmo.backend.evaluator.utils.FilesUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class JavaMetricEvaluator implements MetricEvaluator {

    private static final List<String> JAVA_EXTENSIONS = List.of(".java");

    static public String getPackageName(String filePath) throws MetricEvaluationException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new MetricEvaluationException("Non correct source file " + filePath);
        }

        boolean isInsideComment = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (isInsideComment) {
                    int index;
                    if ((index = trimmed.indexOf("*/")) != -1) {
                        trimmed = trimmed.substring(index + 2).trim();
                        isInsideComment = false;
                    } else {
                        continue;
                    }
                }

                if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                    continue;
                }

                int index = 0;
                while (trimmed.startsWith("/*") && (index = trimmed.indexOf("*/", 2)) != -1) {
                    trimmed = trimmed.substring(index + 2).trim();
                }

                if (trimmed.isEmpty()) {
                    continue;
                }

                if (trimmed.startsWith("/*")) {
                    isInsideComment = true;
                    continue;
                }


                // First statement in code should be `package` statement
                if (trimmed.startsWith("package ")) {
                    int semicolon = trimmed.indexOf(";");
                    if (semicolon == -1) {
                        throw new MetricEvaluationException("Multiline package declaration is not supported");
                    }
                    return trimmed.substring("package ".length(), semicolon).trim();
                } else {
                    return "<default>";
                }
            }
        } catch (FileNotFoundException e) {
            throw new MetricEvaluationException("File not found", e);
        } catch (IOException e) {
            throw new MetricEvaluationException(e);
        }

        return "<default>";
    }
    private record ClassMethod(String packageName, String className, String simpleClassName, String methodName) {
    }

    private ClassMethod parseMethodName(String methodName, Map<String, String> metrics) throws MetricEvaluationException {
        String sourceFileName = metrics.get(LizardRunner.LizardFields.FILE);
        String packageName = getPackageName(sourceFileName);
        String[] parts = methodName.split("::");
        String simpleMethodName = parts[parts.length - 1];
        String fullClassName = packageName + "." + (parts.length == 1 ? "<unnamed>" : parts[0]);
        return new ClassMethod(packageName, fullClassName, parts[0], simpleMethodName);
    }

    Map<String, ClassMetric> lizardOutputToJavaMetrics(Map<String, Map<String, String>> methods) throws MetricEvaluationException {
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
    public Map<String, ClassMetric> evaluateMetrics(File repository, Predicate<Path> filesFilter, List<String> metrics, MetricEvaluationContext context) throws MetricEvaluationException {
        Predicate<Path> filter = FilesUtils.haveExtension(JAVA_EXTENSIONS).and(filesFilter);
        List<Path> filesToProcess = null;

        try {
            filesToProcess = FilesUtils.collectPaths(repository, filter);
        } catch (IOException e) {
            throw new MetricEvaluationException(e);
        }

        var lizardOutput = LizardRunner.runLizard("java", filesToProcess);
        if (context.repoUrl() != null) {
            LizardRunner.generateGithubLocations(lizardOutput, context.repoUrl());
        }
        return lizardOutputToJavaMetrics(lizardOutput);
    }
}
