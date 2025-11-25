package ru.itmo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.request.gitproject.ProjectUrlDTO;
import ru.itmo.backend.dto.response.gitproject.ProjectResponseDTO;
import ru.itmo.backend.service.downloader.GitProjectService;

@RestController
@RequestMapping("/project")
@Tag(name = "Repository Management", description = "Endpoints for cloning and managing repositories")
public class GitProjectController {

    private final GitProjectService gitProjectService;

    public GitProjectController(GitProjectService gitProjectService) {
        this.gitProjectService = gitProjectService;
    }

    @Operation(summary = "Clone or retrieve a repository")
    @PostMapping("/clone")
    public ProjectResponseDTO cloneRepository(@RequestBody ProjectUrlDTO request) throws Exception {
        return gitProjectService.getOrCloneProject(request.url());
    }
}