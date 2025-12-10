package ru.itmo.backend.evaluator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.itmo.backend.evaluator.evaluators.JavaMetricEvaluator;

class JavaMetricEvaluatorTest {

    @Test
    void getPackageName() {

        try {
            String filePath = "src/test/test-data/java/PackageName.java";
            Assertions.assertEquals("test.name", JavaMetricEvaluator.getPackageName(filePath));
        } catch (MetricEvaluationException e) {
            Assertions.fail(e);
        }

    }
}