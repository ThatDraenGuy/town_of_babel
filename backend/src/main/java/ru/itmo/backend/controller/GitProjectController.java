package ru.itmo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.request.ProjectUrlDTO;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.service.downloader.GitProjectService;

import java.util.Map;

@RestController
@RequestMapping("/project")
@Tag(name = "Repository Management", description = "Endpoints for cloning and managing repositories")
public class GitProjectController {

    private final GitProjectService gitProjectService;

    public GitProjectController(GitProjectService gitProjectService) {
        this.gitProjectService = gitProjectService;
    }

    @Operation(
            summary = "Clone or retrieve a repository",
            description = "Clones the project if not cached or retrieves existing cached project",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Project retrieved/cloned successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid URL"),
                    @ApiResponse(responseCode = "500", description = "Server error")
            }
    )
    @PostMapping("/clone")
    public Map<String, Object> cloneRepository(@RequestParam ProjectUrlDTO request) throws Exception {
        GitProjectEntity entity = gitProjectService.getOrCloneProject(request.url());
        Map<String, String> parsed = gitProjectService.parseGithubUrl(request.url());
        return Map.of(
                "projectId", entity.getId(),
                "owner", parsed.get("owner"),
                "project", parsed.get("repo"),
                "path", entity.getLocalPath()
        );
    }
}