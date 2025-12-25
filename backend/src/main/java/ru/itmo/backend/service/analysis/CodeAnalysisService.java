package ru.itmo.backend.service.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ru.itmo.backend.dto.response.analysis.*;
import ru.itmo.backend.dto.response.commit.CommitDTO;
import ru.itmo.backend.dto.response.reference.MetricDTO;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.entity.ProjectInstanceEntity;
import ru.itmo.backend.service.ProjectInstanceArbitrator;
import ru.itmo.backend.service.downloader.GitClient;
import ru.itmo.backend.evaluator.MetricEvaluationException;
import ru.itmo.backend.evaluator.MetricEvaluator;
import ru.itmo.backend.evaluator.MetricEvaluators;
import ru.itmo.backend.evaluator.model.ClassMetric;
import ru.itmo.backend.service.reference.ReferenceProperties;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for code analysis.
 * <p>
 * Currently, provides stubbed (fake) metrics for testing purposes.
 * In the future, real analysis logic (AST, metrics, complexity) will replace the stubs.
 */
@Service
public class CodeAnalysisService {
   static final List<String> METRICS_LIST = List.of("NLOC"); // TODO: merge with language DTO

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(CodeAnalysisService.class);
    private final Random random = new Random();
    private final ProjectInstanceArbitrator arbitrator;
    private final GitClient gitClient;
    private final ReferenceProperties referenceProperties;


    public CodeAnalysisService(ProjectInstanceArbitrator arbitrator, GitClient gitClient, ReferenceProperties referenceProperties) {
        this.arbitrator = arbitrator;
        this.gitClient = gitClient;
        this.referenceProperties = referenceProperties;
    }

    /**
     * Returns metrics for a specific commit.
     */
    public CommitMetricsDTO getCommitMetrics(GitProjectEntity project, CommitDTO commit, List<String> metrics) throws Exception {
        ProjectInstanceEntity instance = arbitrator.acquireInstance(project.getId());
        try {
            File projectDir = new File(instance.getLocalPath());
            gitClient.checkout(projectDir, commit.sha());
            log.info("Analyzing commit {} of project {} using instance {}", commit.sha(), project.getId(), instance.getId());
            
            // Determine language - use project language or default to Java
            String languageName = project.getLanguageCode() != null ? project.getLanguageCode() : "Java";
            MetricEvaluators.Language language = MetricEvaluators.Language.ofName(languageName);
            MetricEvaluator evaluator = MetricEvaluators.forLanguage(language);
            
            Map<String, ClassMetric> classMetrics = evaluator.evaluateMetrics(projectDir, e -> true, metrics);
            
            // Convert ClassMetric map to MetricsNodeDTO tree
            MetricsNodeDTO root = convertClassMetricsToTree(project, classMetrics, metrics);
            
            return new CommitMetricsDTO(commit, root);
        } finally {
            arbitrator.releaseInstance(instance.getId());
        }
    }

    private record Package(String name, Map<String, Package> packages, List<ClassMetricsNodeDTO> classes) {
        PackageMetricsNodeDTO toNode() {
            var packageNodes = packages.values().stream().map(Package::toNode).toList();
            if (classes.isEmpty() && packageNodes.size() == 1) {
                var inner = packageNodes.getFirst();
                if (name.equals("<root>")) return inner;
                return new PackageMetricsNodeDTO(name + "." + inner.name(), inner.items());
            }
            List<MetricsNodeDTO> nodes = new ArrayList<>();
            nodes.addAll(packageNodes);
            nodes.addAll(classes);
            return new PackageMetricsNodeDTO(name, nodes);
        }
    };
    /**
     * Converts a map of ClassMetric to a MetricsNodeDTO tree structure.
     * Groups classes by package and creates a hierarchical structure.
     */
    private MetricsNodeDTO convertClassMetricsToTree(GitProjectEntity project, Map<String, ClassMetric> classMetrics, List<String> requestedMetrics) {
        if (classMetrics.isEmpty()) {
            // Return empty package if no classes found
            return new PackageMetricsNodeDTO("", List.of());
        }

        var metricsByCode = referenceProperties.getLanguages().stream()
                .filter(lang -> lang.getLanguage().equals(project.getLanguageCode()))
                .flatMap(lang -> lang.getMetrics().stream())
                .collect(Collectors.toMap(ReferenceProperties.MetricConfig::getId, Function.identity()));
        // Group classes by package
        Map<String, List<ClassMetricsNodeDTO>> packageMap = new HashMap<>();
        
        for (ClassMetric classMetric : classMetrics.values()) {
            String className = classMetric.name();
            String packageName = "";
            String simpleClassName = className;
            
            // Extract package name from fully qualified class name
            int lastDot = className.lastIndexOf('.');
            if (lastDot >= 0) {
                packageName = className.substring(0, lastDot);
                simpleClassName = className.substring(lastDot + 1);
            }
            
            // Convert methods to MethodMetricsNodeDTO
            List<MethodMetricsNodeDTO> methodNodes = classMetric.methods().values().stream()
                .map(method -> {
                    List<MethodMetricDTO> methodMetrics = new ArrayList<>();
                    for (String metricCode : requestedMetrics) {
                        var metric = metricsByCode.get(metricCode);
                        switch (metric.getType()) {
                            case COLOR -> {
                                if (metricCode.equals("PARAM_COLOR")) {
                                    String valueStr = method.own().get("PARAM");
                                    try {
                                        String value = getColoredParam(valueStr);
                                        methodMetrics.add(new MethodMetricDTO(metricCode, null, null,
                                                new MethodMetricDTO.ColorValue(value, valueStr)));
                                    } catch (NumberFormatException e) {
                                        log.warn("Could not parse metric value {} for metric {}: {}", valueStr, metricCode, e.getMessage());
                                    }
                                }
                            }
                            case STRING -> {
                                String value = method.own().get(metricCode);
                                methodMetrics.add(new MethodMetricDTO(metricCode, value, null, null));
                            }
                            case NUMERIC -> {
                                String valueStr = method.own().get(metricCode);
                                try {
                                    Integer value = Integer.parseInt(valueStr);
                                    methodMetrics.add(new MethodMetricDTO(metricCode, null, value, null));
                                } catch (NumberFormatException e) {
                                    log.warn("Could not parse metric value {} for metric {}: {}", valueStr, metricCode, e.getMessage());
                                }
                            }
                        }
                    }
                    return new MethodMetricsNodeDTO(method.name(), methodMetrics);
                })
                .toList();
            
            ClassMetricsNodeDTO classNode = new ClassMetricsNodeDTO(simpleClassName, methodNodes);
            packageMap.computeIfAbsent(packageName, k -> new ArrayList<>()).add(classNode);
        }
        
        // Create package nodes
        Package root = new Package("<root>", new HashMap<>(), new ArrayList<>());
        for (Map.Entry<String, List<ClassMetricsNodeDTO>> entry : packageMap.entrySet()) {
            String packageName = entry.getKey();
            String[] steps = packageName.split("\\.");
            Package currentPackage = root;
            for (String step : steps) {
                currentPackage = currentPackage.packages.computeIfAbsent(step, k -> new Package(k, new HashMap<>(), new ArrayList<>()));
            }
            List<ClassMetricsNodeDTO> classes = entry.getValue();
            currentPackage.classes.addAll(classes);
        }

        return root.toNode();
    }

    private static String getColoredParam(String valueStr) {
        int valueInt = Integer.parseInt(valueStr);
        return switch (valueInt) {
            case 0 -> "0x34e8eb";
            case 1 -> "0x64eb34";
            case 2 -> "0xdeeb34";
            case 3 -> "0xeb9b34";
            default -> "0xeb3d34";
        };
    }

    /**
     * Returns the most popular language based on GitHub stats
     *
     * @param threshold - share of language to be considered as most popular
     * @return Name of the most popular language if present
     */
    public Optional<String> getMostPopularLanguageFromGitHub(String gitProjectUrl, double threshold) throws IOException, InterruptedException {
        String path = URI.create(gitProjectUrl).getPath();
        String[] parts = path.substring(1).split("/");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub repository URL");
        }

        String owner = parts[0];
        String repo = parts[1].replace(".git", "");
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/languages", owner, repo);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).header("Accept", "application/vnd.github.v3+json").build();

        System.out.println(request.toString());

        String jsonResponse = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();

        System.out.println(jsonResponse);

        TypeReference<Map<String, Integer>> typeRef = new TypeReference<>() {
        };
        Map<String, Integer> languages = MAPPER.readValue(jsonResponse, typeRef);

        long totalLines = languages.values().stream().mapToLong(Integer::longValue).sum();

        return languages.entrySet().stream()
                .filter(entry -> (double) entry.getValue() / totalLines > threshold)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    public Optional<String> getMostPopularLanguageFromGitHub(String gitProjectUrl) throws IOException, InterruptedException {
        return getMostPopularLanguageFromGitHub(gitProjectUrl, 0.7);
    }


    /**
     * Analyzes the code of the given project directory at the current HEAD using a free instance.
     *
     * @param projectId project ID
     * @param languages list of languages to analyse
     * @return map of analysis metrics as JSON string
     */
    public String analyzeProject(Long projectId, List<String> languages) throws Exception {
        ProjectInstanceEntity instance = arbitrator.acquireInstance(projectId);
        try {
            File projectDir = new File(instance.getLocalPath());
            log.info("Analyzing project {} using instance {}", projectId, instance.getId());
            return analyzeProject(projectDir, languages);
        } finally {
            arbitrator.releaseInstance(instance.getId());
        }
    }

    /**
     * Analyzes the code of the given project directory at the current HEAD.
     *
     * @param projectDir the root directory of the project
     * @param languages list of languages to analyse
     * @return map of analysis metrics
     */
    public String analyzeProject(File projectDir, List<String> languages) throws MetricEvaluationException, IOException {
        log.info("Analyzing project at path: {}", projectDir.getAbsolutePath());

        Map<String, Map<String, ClassMetric>> metrics = new HashMap<>();

        for (String languageName : languages) {
            MetricEvaluators.Language language = MetricEvaluators.Language.ofName(languageName);
            MetricEvaluator evaluator = MetricEvaluators.forLanguage(language);
            metrics.put(languageName, evaluator.evaluateMetrics(projectDir, e -> true, METRICS_LIST));
        }

        return MAPPER.writeValueAsString(metrics);
    }



    /**
     * Analyzes a specific branch of a project.
     *
     * @param projectDir the root directory of the project
     * @param branchName branch name to analyze
     * @return map of analysis metrics
     */
    public Map<String, Object> analyzeBranch(File projectDir, String branchName) throws MetricEvaluationException, IOException {
        log.info("Analyzing branch '{}' in project at path: {}", branchName, projectDir.getAbsolutePath());
        
        // Try to determine language - default to Java if unknown
        String languageName = "Java";
        try {
            MetricEvaluators.Language language = MetricEvaluators.Language.ofName(languageName);
            MetricEvaluator evaluator = MetricEvaluators.forLanguage(language);
            Map<String, ClassMetric> metrics = evaluator.evaluateMetrics(projectDir, e -> true, METRICS_LIST);
            
            int totalFiles = metrics.size();
            int totalMethods = metrics.values().stream()
                .mapToInt(c -> c.methods().size())
                .sum();
            
            return Map.of(
                "branch", branchName,
                "total_files", totalFiles,
                "classes_count", totalFiles,
                "methods_count", totalMethods
            );
        } catch (MetricEvaluationException e) {
            log.warn("Failed to analyze branch {}: {}", branchName, e.getMessage());
            throw e;
        }
    }

    /**
     * Analyzes a specific commit of a project using a free instance.
     *
     * @param projectId project ID
     * @param commitSha SHA of the commit to analyze
     * @return map of analysis metrics
     */
    public Map<String, Object> analyzeCommit(Long projectId, String commitSha) throws Exception {
        ProjectInstanceEntity instance = arbitrator.acquireInstance(projectId);
        try {
            File projectDir = new File(instance.getLocalPath());
            gitClient.checkout(projectDir, commitSha);
            log.info("Analyzing commit {} of project {} using instance {}", commitSha, projectId, instance.getId());
            return analyzeCommit(projectDir, commitSha);
        } finally {
            arbitrator.releaseInstance(instance.getId());
        }
    }

    /**
     * Analyzes a specific commit of a project.
     *
     * @param projectDir the root directory of the project
     * @param commitSha  SHA of the commit to analyze
     * @return map of analysis metrics
     */
    public Map<String, Object> analyzeCommit(File projectDir, String commitSha) throws MetricEvaluationException, IOException {
        log.info("Analyzing commit '{}' in project at path: {}", commitSha, projectDir.getAbsolutePath());
        
        // Try to determine language - default to Java if unknown
        String languageName = "Java";
        try {
            MetricEvaluators.Language language = MetricEvaluators.Language.ofName(languageName);
            MetricEvaluator evaluator = MetricEvaluators.forLanguage(language);
            Map<String, ClassMetric> metrics = evaluator.evaluateMetrics(projectDir, e -> true, METRICS_LIST);
            
            int totalFiles = metrics.size();
            int totalMethods = metrics.values().stream()
                .mapToInt(c -> c.methods().size())
                .sum();
            
            return Map.of(
                "commit_sha", commitSha,
                "total_files", totalFiles,
                "classes_count", totalFiles,
                "methods_count", totalMethods
            );
        } catch (MetricEvaluationException e) {
            log.warn("Failed to analyze commit {}: {}", commitSha, e.getMessage());
            throw e;
        }
    }

    /**
     * Computes a diff analysis between two commits using a free instance.
     *
     * @param projectId project ID
     * @param oldCommitSha SHA of the older commit
     * @param newCommitSha SHA of the newer commit
     * @return map of delta metrics
     */
    public Map<String, Object> analyzeDiff(Long projectId, String oldCommitSha, String newCommitSha) throws Exception {
        ProjectInstanceEntity instance = arbitrator.acquireInstance(projectId);
        try {
            File projectDir = new File(instance.getLocalPath());
            // Diff usually doesn't need checkout, but stub might.
            log.info("Analyzing diff {}..{} of project {} using instance {}", oldCommitSha, newCommitSha, projectId, instance.getId());
            return analyzeDiff(projectDir, oldCommitSha, newCommitSha);
        } finally {
            arbitrator.releaseInstance(instance.getId());
        }
    }

    /**
     * Computes a diff analysis between two commits.
     *
     * @param projectDir   the root directory of the project
     * @param oldCommitSha SHA of the older commit
     * @param newCommitSha SHA of the newer commit
     * @return map of delta metrics
     */
    public Map<String, Object> analyzeDiff(File projectDir, String oldCommitSha, String newCommitSha) {
        log.info("Stub diff analysis between '{}' and '{}' in project at path: {}", oldCommitSha, newCommitSha, projectDir.getAbsolutePath());

        return Map.of("old_commit", oldCommitSha, "new_commit", newCommitSha, "total_files_delta", random.nextInt(5) - 2, "java_lines_delta", random.nextInt(50) - 25, "methods_count_delta", random.nextInt(3) - 1, "classes_count_delta", random.nextInt(2) - 1);
    }
}
