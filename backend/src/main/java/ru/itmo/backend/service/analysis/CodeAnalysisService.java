package ru.itmo.backend.service.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ru.itmo.backend.dto.response.analysis.*;
import ru.itmo.backend.dto.response.commit.CommitDTO;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.entity.ProjectInstanceEntity;
import ru.itmo.backend.service.ProjectInstanceArbitrator;
import ru.itmo.backend.service.downloader.GitClient;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

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

    public CodeAnalysisService(ProjectInstanceArbitrator arbitrator, GitClient gitClient) {
        this.arbitrator = arbitrator;
        this.gitClient = gitClient;
    }

    /**
     * Returns metrics for a specific commit.
     */
    public CommitMetricsDTO getCommitMetrics(GitProjectEntity project, CommitDTO commit, List<String> metrics) {
        // Stub implementation creating a dummy tree structure
        MethodMetricsNodeDTO method1 = new MethodMetricsNodeDTO("calculateSum", 
            metrics.stream().map(m -> new MethodMetricDTO(m, 10.0 + random.nextInt(20))).toList());
        MethodMetricsNodeDTO method2 = new MethodMetricsNodeDTO("printResult", 
            metrics.stream().map(m -> new MethodMetricDTO(m, 5.0 + random.nextInt(10))).toList());
        
        ClassMetricsNodeDTO class1 = new ClassMetricsNodeDTO("Calculator", List.of(method1, method2));
        
        PackageMetricsNodeDTO root = new PackageMetricsNodeDTO("ru.itmo.backend", List.of(class1));
        
        return new CommitMetricsDTO(commit, root);
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
    public String analyzeProject(File projectDir, List<String> languages) throws IOException {
        log.info("Stub analysis of project at path: {}", projectDir.getAbsolutePath());

        // Returning stub data to keep tests passing without external tools like lizard
        Map<String, Object> stubMetrics = new HashMap<>();
        for (String lang : languages) {
            stubMetrics.put(lang, Map.of(
                "NLOC", 100 + random.nextInt(1000),
                "Complexity", 5 + random.nextInt(20),
                "Files", 10 + random.nextInt(50)
            ));
        }

        return MAPPER.writeValueAsString(stubMetrics);
    }



    /**
     * Analyzes a specific branch of a project.
     *
     * @param projectDir the root directory of the project
     * @param branchName branch name to analyze
     * @return map of analysis metrics
     */
    public Map<String, Object> analyzeBranch(File projectDir, String branchName) {
        log.info("Stub analysis of branch '{}' in project at path: {}", branchName, projectDir.getAbsolutePath());

        return Map.of("branch", branchName, "total_files", 50 + random.nextInt(20), "java_lines", 300 + random.nextInt(50), "methods_count", 10 + random.nextInt(5), "classes_count", 5 + random.nextInt(5));
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
    public Map<String, Object> analyzeCommit(File projectDir, String commitSha) {
        log.info("Stub analysis of commit '{}' in project at path: {}", commitSha, projectDir.getAbsolutePath());

        return Map.of("commit_sha", commitSha, "total_files", 45 + random.nextInt(20), "java_lines", 280 + random.nextInt(60), "methods_count", 8 + random.nextInt(6), "classes_count", 4 + random.nextInt(5));
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
