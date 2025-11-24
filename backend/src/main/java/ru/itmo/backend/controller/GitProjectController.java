package ru.itmo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.request.AnalyzeRequest;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.service.downloader.GitProjectService;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/project")
@Tag(
        name = "Git project API",
        description = "Endpoints for cloning, caching, and analyzing GitHub projects"
)
public class GitProjectController {

    private final GitProjectService gitProjectService;

    /** Regex that supports:
     *  - https://github.com/user/repo
     *  - https://github.com/user/repo.git
     *  - git@github.com:user/repo.git
     *  - ssh://git@github.com/user/repo.git
     */
    private static final Pattern GITHUB_REGEX =
            Pattern.compile("github\\.com[:/](.+?)/(.+?)(\\.git)?$");

    public GitProjectController(GitProjectService gitProjectService) {
        this.gitProjectService = gitProjectService;
    }

    @Operation(
            summary = "Analyze a GitHub project",
            description = """
                Clones the project if it is not cached yet, stores metadata in the database,
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
                                          "projectId": 12,
                                          "project": "my-repo",
                                          "owner": "octocat",
                                          "path": "/tmp/projects/repo-12",
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

        GitProjectEntity entity = gitProjectService.getOrCloneProject(request.url());
        Map<String, Object> analysis = gitProjectService.analyzeProject(new File(entity.getLocalPath()));

        return Map.of(
                "projectId", entity.getId(),
                "project", extractRepositoryName(request.url()),
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
