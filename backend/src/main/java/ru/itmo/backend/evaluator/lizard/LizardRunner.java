package ru.itmo.backend.evaluator.lizard;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import ru.itmo.backend.evaluator.MetricEvaluationException;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LizardRunner {

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

    public static void generateGithubLocations(Map<String, Map<String, String>> lizardOutput, String repoUrl) throws MetricEvaluationException {
        for (var methodStats : lizardOutput.entrySet()) {
            String[] locationParts = methodStats.getValue().get(LizardFields.LOCATION).split("@");
            if (locationParts.length != 3) {
                throw new MetricEvaluationException("Cannot parse location: " + methodStats.getValue().get(LizardFields.LOCATION));
            }
            String[] linesNumber = locationParts[1].split("-");
            if (linesNumber.length != 2) {
                throw new MetricEvaluationException("Cannot parse location: " + methodStats.getValue().get(LizardFields.LOCATION));
            }

            String normalizedRepoUrl = repoUrl.trim();
            if (normalizedRepoUrl.endsWith("/")) {
                normalizedRepoUrl = normalizedRepoUrl.substring(0, normalizedRepoUrl.length() - 1);
            }
            if (normalizedRepoUrl.endsWith(".git")) {
                normalizedRepoUrl = normalizedRepoUrl.substring(0, normalizedRepoUrl.length() - 4);
            }
            if (!normalizedRepoUrl.contains("github.com")) {
                throw new MetricEvaluationException("Non Github-URL for GitHub location");
            }
            String normalizedFilePath = locationParts[2].trim();
            if (normalizedFilePath.startsWith("/")) {
                normalizedFilePath = normalizedFilePath.substring(1);
            }
            String encodedFilePath = URLEncoder.encode(normalizedFilePath, StandardCharsets.UTF_8).replace("%2F", "/").replace("+", "%20");

            StringBuilder urlBuilder = new StringBuilder(normalizedRepoUrl);
            if (!normalizedRepoUrl.contains("/blob/") && !normalizedRepoUrl.contains("/tree/")) {
                urlBuilder.append("/blob/main/");
            } else {
                if (!normalizedRepoUrl.endsWith("/")) {
                    urlBuilder.append("/");
                }
            }

            urlBuilder.append(encodedFilePath);
            urlBuilder.append("#L").append(linesNumber[0]).append("-L").append(linesNumber[1]);
            methodStats.getValue().put(LizardFields.GITHUB_LINK, urlBuilder.toString());
        }
    }

    public static Map<String, Map<String, String>> parseLizardOutput(Reader rawReader) throws IOException, MetricEvaluationException {
        Map<String, Map<String, String>> result = new HashMap<>();


        CSVParser parser = new CSVParserBuilder().withEscapeChar('\0').build();

        try (CSVReader reader = new CSVReaderBuilder(rawReader).withCSVParser(parser).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> methodMetrics = new HashMap<>();

                for (int i = 0; i < LizardFields.NATIVE_FIELDS.size(); i++) {
                    methodMetrics.put(LizardFields.NATIVE_FIELDS.get(i), line[i]);
                }

                result.put(methodMetrics.get(LizardFields.FULL_NAME), methodMetrics);
            }

        } catch (CsvValidationException e) {
            throw new MetricEvaluationException("Lizard output parse failed", e);
        }
        return result;
    }

    static private class TemporaryFile implements AutoCloseable {
        private final Path file;

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

        public static final String GITHUB_LINK = "GITHUB_LINK";

        public static final List<String> NATIVE_FIELDS = List.of(NLOC, CCN, TKN, PARAM, LENGTH, LOCATION, FILE, FULL_NAME);
        public static final List<String> METRICS = Arrays.asList(NLOC, CCN, PARAM, LENGTH, FILE);
        public static final List<String> GENERATED_FIELDS = List.of(GITHUB_LINK);

        public static Map<String, String> filterMetrics(Map<String, String> stats) {
            return stats.entrySet().stream().filter(e -> METRICS.contains(e.getKey())).collect(Collectors.toMap(Map.Entry<String, String>::getKey, Map.Entry<String, String>::getValue));
        }
    }
}
