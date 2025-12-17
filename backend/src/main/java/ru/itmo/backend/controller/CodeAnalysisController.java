package ru.itmo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.service.analysis.CodeAnalysisService;
import ru.itmo.backend.service.downloader.ProjectAccessService;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/analysis")
@Tag(name = "Code Analysis", description = "Endpoints for analyzing project code")
public class CodeAnalysisController {

    private final CodeAnalysisService codeAnalysisService;
    private final ProjectAccessService projectAccessService;

    public CodeAnalysisController(CodeAnalysisService codeAnalysisService,
                                  ProjectAccessService projectAccessService) {
        this.codeAnalysisService = codeAnalysisService;
        this.projectAccessService = projectAccessService;
    }

    @Operation(summary = "Analyze a project",
            description = "Performs static code analysis on the entire cloned project (last commit on main)")
    @PostMapping("/project/{projectId}")
    public Map<String, Object> analyzeProject(@PathVariable Long projectId) throws Exception {
        GitProjectEntity project = projectAccessService.getById(projectId);
        return codeAnalysisService.analyzeProject(new File(project.getLocalPath()));
    }

    @Operation(summary = "Analyze a specific commit",
            description = "Performs static code analysis on the code at a given commit")
    @PostMapping("/project/{projectId}/commit/{commitSha}")
    public Map<String, Object> analyzeCommit(@PathVariable Long projectId,
                                             @PathVariable String commitSha) throws Exception {
        GitProjectEntity project = projectAccessService.getById(projectId);
        return codeAnalysisService.analyzeCommit(new File(project.getLocalPath()), commitSha);
    }

    @Operation(summary = "Analyze diff between commits",
            description = "Performs analysis on the diff between two commits")
    @PostMapping("/project/{projectId}/diff")
    public Map<String, Object> analyzeDiff(@PathVariable Long projectId,
                                           @RequestParam String baseCommit,
                                           @RequestParam String targetCommit) throws Exception {
        GitProjectEntity project = projectAccessService.getById(projectId);
        return codeAnalysisService.analyzeDiff(
                new File(project.getLocalPath()), baseCommit, targetCommit);
    }
}
