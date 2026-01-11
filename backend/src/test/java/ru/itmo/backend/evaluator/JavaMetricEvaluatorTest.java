package ru.itmo.backend.evaluator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import ru.itmo.backend.evaluator.evaluators.JavaMetricEvaluator;
import ru.itmo.backend.evaluator.model.ClassMetric;

import java.io.File;
import java.io.IOException;
import java.util.List;

class JavaMetricEvaluatorTest {

    private boolean isLizardAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("lizard --version");
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Test
    void getPackageName() {

        try {
            String filePath = "src/test/test-data/java/PackageName.java";
            Assertions.assertEquals("test.name", JavaMetricEvaluator.getPackageName(filePath));
        } catch (MetricEvaluationException e) {
            Assertions.fail(e);
        }
    }

    @Test
    void runMetrics() {
        Assumptions.assumeTrue(isLizardAvailable(), "Lizard tool is not available in the environment");
        try {
            File repo = new File("src/test/test-data/java/SmokeTest/");
            JavaMetricEvaluator evaluator = new JavaMetricEvaluator();
            var metrics = evaluator.evaluateMetrics(repo, path -> true, List.of());
            Assertions.assertTrue(metrics.containsKey("test.test.a.A"));
            ClassMetric aMetric = metrics.get("test.test.a.A");
            Assertions.assertEquals(2, aMetric.methods().size());
            Assertions.assertEquals("5", aMetric.methods().get("foo").own().get("CCN"));
        } catch (MetricEvaluationException | IOException e) {
            Assertions.fail(e);
        }
    }
}