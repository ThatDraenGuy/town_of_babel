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
//                System.out.print(line.getKey() + " : ");
//                for (var entry : line.getValue().entrySet()) {
//                    System.out.print(entry.getKey() + " = " + entry.getValue() + ", ");
//                }
//                System.out.println();
            }
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    void runLizard() {
        try {
            List<Path> paths = FilesUtils.collectPaths(new File("src/test/test-data/java/SmokeTest"), a -> true);
            Assertions.assertEquals(1, paths.size());

            var metrics = LizardRunner.runLizard("java", paths);

            Assertions.assertEquals(2, metrics.size());

//            for (Map<String, String> rec : metrics.values()) {
//                System.out.println(rec.size());
//                for (var entry : rec.entrySet()) {
//                    System.out.println("<(" + entry.getKey() + "):(" + entry.getValue() + ")>");
//                }
//            }
        } catch (MetricEvaluationException | IOException e) {
            Assertions.fail(e);
        }
    }
}