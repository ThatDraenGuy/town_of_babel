package ru.itmo.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.AnalyzeRequest;
import ru.itmo.backend.service.GitRepositoryService;
import ru.itmo.backend.entity.GitRepositoryEntity;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/repository")
public class GitRepositoryController {

    private final GitRepositoryService gitRepositoryService;

    /** Regex that supports:
     *  - https://github.com/user/repo
     *  - https://github.com/user/repo.git
     *  - git@github.com:user/repo.git
     *  - ssh://git@github.com/user/repo.git
     */
    private static final Pattern GITHUB_REGEX =
            Pattern.compile("github\\.com[:/](.+?)/(.+?)(\\.git)?$");

    public GitRepositoryController(GitRepositoryService gitRepositoryService) {
        this.gitRepositoryService = gitRepositoryService;
    }

    /**
     * Analyzes a GitHub repository: clones it if needed and performs static analysis.
     *
     * @param request container with repository URL
     * @return JSON with repository metadata and analysis results
     * @throws Exception if clone or analysis fails
     */
    @PostMapping("/analyze")
    public Map<String, Object> analyzeRepository(@RequestBody AnalyzeRequest request) throws Exception {

        GitRepositoryEntity entity = gitRepositoryService.getOrCloneRepository(request.url());
        Map<String, Object> analysis = gitRepositoryService.analyzeRepository(new File(entity.getLocalPath()));

        return Map.of(
                "repositoryId", entity.getId(),
                "repository", extractRepositoryName(request.url()),
                "owner", extractOwner(request.url()),
                "path", entity.getLocalPath(),
                "analysis", analysis
        );
    }

    /**
     * Extracts the repository name from a GitHub URL.
     *
     * @param url GitHub repository URL
     * @return repository name or "unknown" if invalid format
     */
    private String extractRepositoryName(String url) {
        Matcher m = GITHUB_REGEX.matcher(url);
        return m.find() ? m.group(2) : "unknown";
    }

    /**
     * Extracts the repository owner (organization/user) from a GitHub URL.
     *
     * @param url GitHub repository URL
     * @return repository owner or "unknown"
     */
    private String extractOwner(String url) {
        Matcher m = GITHUB_REGEX.matcher(url);
        return m.find() ? m.group(1) : "unknown";
    }
}
