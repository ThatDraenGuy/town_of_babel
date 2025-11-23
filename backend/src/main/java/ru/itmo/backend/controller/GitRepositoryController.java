package ru.itmo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.AnalyzeRequest;
import ru.itmo.backend.entity.GitRepositoryEntity;
import ru.itmo.backend.service.downloader.GitRepositoryService;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/repository")
@Tag(
        name = "Git Repository API",
        description = "Endpoints for cloning, caching, and analyzing GitHub repositories"
)
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

    @Operation(
            summary = "Analyze a GitHub repository",
            description = """
                Clones the repository if it is not cached yet, stores metadata in the database,
                and performs static analysis (languages, metrics, etc.).
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Analysis successfully completed",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = """
                                        {
                                          "repositoryId": 12,
                                          "repository": "my-repo",
                                          "owner": "octocat",
                                          "path": "/tmp/repositories/repo-12",
                                          "analysis": {
                                            "languages": {
                                              "Java": 12412,
                                              "Python": 9123
                                            }
                                          }
                                        }
                                        """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or malformed GitHub URL"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error"
                    )
            }
    )
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

    private String extractRepositoryName(String url) {
        Matcher m = GITHUB_REGEX.matcher(url);
        return m.find() ? m.group(2) : "unknown";
    }

    private String extractOwner(String url) {
        Matcher m = GITHUB_REGEX.matcher(url);
        return m.find() ? m.group(1) : "unknown";
    }
}
