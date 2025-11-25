package ru.itmo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.request.ProjectUrlDTO;
import ru.itmo.backend.dto.response.ProjectResponseDTO;
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

    @Operation(summary = "Clone or retrieve a repository")
    @PostMapping("/clone")
    public ProjectResponseDTO cloneRepository(@RequestBody ProjectUrlDTO request) throws Exception {
        return gitProjectService.cloneOrGetProject(request.url());
    }
}