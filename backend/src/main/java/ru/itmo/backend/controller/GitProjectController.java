package ru.itmo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.request.gitproject.ProjectRequestDTO;
import ru.itmo.backend.dto.response.gitproject.ProjectResponseDTO;
import ru.itmo.backend.service.downloader.GitProjectService;

@RestController
@RequestMapping("/projects")
@Tag(name = "Repository Management", description = "Endpoints for cloning and managing repositories")
public class GitProjectController {

    private final GitProjectService gitProjectService;

    public GitProjectController(GitProjectService gitProjectService) {
        this.gitProjectService = gitProjectService;
    }

    @Operation(summary = "Clone or retrieve a repository", description = "Clones the project if not cached or retrieves existing cached project", operationId = "cloneProject")
    @PostMapping("/clone")
    public ProjectResponseDTO cloneRepository(@Valid @RequestBody ProjectRequestDTO request) throws Exception {
        // Validation is handled by @Valid annotation and ProjectRequestDTO constraints
        return gitProjectService.getOrCloneProject(request.url());
    }
}