package ru.itmo.backend.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service responsible for code analysis.
 *
 * Currently provides stubbed (fake) metrics for testing purposes.
 * In the future, real analysis logic (AST, metrics, complexity) will replace the stubs.
 */
@Service
public class CodeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(CodeAnalysisService.class);
    private final Random random = new Random();

    /**
     * Analyzes the code of the given project directory at the current HEAD.
     *
     * @param projectDir the root directory of the project
     * @return map of analysis metrics
     */
    public Map<String, Object> analyzeProject(File projectDir) {
        log.info("Stub analysis of project at path: {}", projectDir.getAbsolutePath());

        // Stub metrics
        return Map.of(
                "total_files", 50,
                "java_lines", 320,
                "methods_count", 12,
                "classes_count", 8
        );
    }


    /**
     * Performs project analysis:
     *  - counts total files
     *  - counts Java lines
     *
     * @param projectDir the root of the project on disk
     * @return map containing analysis results
     */
    /*
    public Map<String, Object> analyzeProject(File projectDir) throws IOException {
        AtomicLong totalFiles = new AtomicLong();
        AtomicLong javaLines = new AtomicLong();

        Files.walk(projectDir.toPath()).forEach(path -> {
            try {
                if (Files.isRegularFile(path)) {
                    totalFiles.incrementAndGet();
                    if (path.toString().endsWith(".java")) {
                        javaLines.addAndGet(Files.lines(path).count());
                    }
                }
            } catch (IOException ignored) { }
        });

        return Map.of(
                "total_files", totalFiles.get(),
                "java_lines", javaLines.get()
        );
    }
     */

    /**
     * Analyzes a specific branch of a project.
     *
     * @param projectDir the root directory of the project
     * @param branchName branch name to analyze
     * @return map of analysis metrics
     */
    public Map<String, Object> analyzeBranch(File projectDir, String branchName) {
        log.info("Stub analysis of branch '{}' in project at path: {}", branchName, projectDir.getAbsolutePath());

        return Map.of(
                "branch", branchName,
                "total_files", 50 + random.nextInt(20),
                "java_lines", 300 + random.nextInt(50),
                "methods_count", 10 + random.nextInt(5),
                "classes_count", 5 + random.nextInt(5)
        );
    }

    /**
     * Analyzes a specific commit of a project.
     *
     * @param projectDir the root directory of the project
     * @param commitSha SHA of the commit to analyze
     * @return map of analysis metrics
     */
    public Map<String, Object> analyzeCommit(File projectDir, String commitSha) {
        log.info("Stub analysis of commit '{}' in project at path: {}", commitSha, projectDir.getAbsolutePath());

        return Map.of(
                "commit_sha", commitSha,
                "total_files", 45 + random.nextInt(20),
                "java_lines", 280 + random.nextInt(60),
                "methods_count", 8 + random.nextInt(6),
                "classes_count", 4 + random.nextInt(5)
        );
    }

    /**
     * Computes a diff analysis between two commits.
     *
     * @param projectDir the root directory of the project
     * @param oldCommitSha SHA of the older commit
     * @param newCommitSha SHA of the newer commit
     * @return map of delta metrics
     */
    public Map<String, Object> analyzeDiff(File projectDir, String oldCommitSha, String newCommitSha) {
        log.info("Stub diff analysis between '{}' and '{}' in project at path: {}", oldCommitSha, newCommitSha, projectDir.getAbsolutePath());

        return Map.of(
                "old_commit", oldCommitSha,
                "new_commit", newCommitSha,
                "total_files_delta", random.nextInt(5) - 2,
                "java_lines_delta", random.nextInt(50) - 25,
                "methods_count_delta", random.nextInt(3) - 1,
                "classes_count_delta", random.nextInt(2) - 1
        );
    }
}
