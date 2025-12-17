package ru.itmo.backend.evaluator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.itmo.backend.evaluator.lizard.LizardRunner;
import ru.itmo.backend.evaluator.utils.FilesUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;

class LizardRunnerTest {
    @Test
    void lizardParser() {
        try {
            Reader reader = new FileReader("src/test/test-data/lizard/lizard-output.csv");
            var csv = LizardRunner.parseLizardOutput(reader);

            Assertions.assertEquals(5, csv.size());
            for (var line : csv.entrySet()) {
                Assertions.assertEquals(LizardRunner.LizardFields.ALL_FIELDS.length, line.getValue().size());
            }
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    void runLizard() {
        try {
            List<Path> paths = FilesUtils.collectPaths(new File("src/test/test-data/java/SmokeTest/"), a -> true);
            Assertions.assertEquals(1, paths.size());

            var metrics = LizardRunner.runLizard("java", paths);
            System.out.println(metrics);

            Assertions.assertEquals(2, metrics.size());
        } catch (MetricEvaluationException | IOException e) {
            Assertions.fail(e);
        }
    }
}