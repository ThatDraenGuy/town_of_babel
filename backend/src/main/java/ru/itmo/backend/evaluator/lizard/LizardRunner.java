package ru.itmo.backend.evaluator.lizard;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import ru.itmo.backend.evaluator.MetricEvaluationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LizardRunner {

    static private class TemporaryFile implements AutoCloseable {
        public TemporaryFile(List<Path> paths) throws IOException {
            this.file = Files.createTempFile("lizard-config", ".txt");
            try (PrintWriter writer = new PrintWriter(Files.newOutputStream(this.file))) {
                for (Path path : paths) {
                    writer.println(path.toString());
                }
                writer.flush();
            }
        }

        Path getPath() {
            return file;
        }

        @Override
        public void close() throws Exception {
            Files.delete(file);
        }

        private final Path file;
    }

    public static Map<String, Map<String, String>> runLizard(String language, List<Path> paths) throws MetricEvaluationException {
        try (var tmp = new TemporaryFile(paths)) {
            String[] command = {"lizard", "-l", language, "--csv", "-f", tmp.getPath().toString()};
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return parseLizardOutput(reader);
            }
        } catch (Exception e) {
            throw new MetricEvaluationException("Lizard run failed...", e);
        }
    }

    public static class LizardFields {
        public static final String NLOC = "NLOC";
        public static final String CCN = "CCN";
        public static final String TKN = "TKN";
        public static final String PARAM = "PARAM";
        public static final String LENGTH = "LENGTH";

        public static final String LOCATION = "LOCATION";
        public static final String FILE = "FILE";
        public static final String FULL_NAME = "FULL_NAME";

        public static final String[] ALL_FIELDS = {NLOC, CCN, TKN, PARAM, LENGTH, LOCATION, FILE, FULL_NAME};
        public static final List<String> METRICS = Arrays.asList(NLOC, CCN, PARAM, LENGTH, FILE);

        public static Map<String, String> filterMetrics(Map<String, String> stats) {
            return stats.entrySet().stream()
                    .filter(e -> METRICS.contains(e.getKey()))
                    .collect(Collectors.toMap(
                            Map.Entry<String, String>::getKey,
                            Map.Entry<String, String>::getValue));
        }
    }

    public static Map<String, Map<String, String>> parseLizardOutput(Reader rawReader) throws IOException, MetricEvaluationException {
        Map<String, Map<String, String>> result = new HashMap<>();


        CSVParser parser = new CSVParserBuilder()
                .withEscapeChar('\0')
                .build();

        try (CSVReader reader = new CSVReaderBuilder(rawReader).withCSVParser(parser).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> methodMetrics = new HashMap<>();

                for (int i = 0; i < LizardFields.ALL_FIELDS.length; i++) {
                    methodMetrics.put(LizardFields.ALL_FIELDS[i], line[i]);
                }

                result.put(methodMetrics.get(LizardFields.FULL_NAME), methodMetrics);
            }

        } catch (CsvValidationException e) {
            throw new MetricEvaluationException("Output parse failed", e);
        }
        return result;
    }
}
