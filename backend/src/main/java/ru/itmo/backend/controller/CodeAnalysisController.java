package ru.itmo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.service.analysis.CodeAnalysisService;
import ru.itmo.backend.service.downloader.ProjectAccessService;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/analysis")
@Tag(name = "Code Analysis", description = "Endpoints for analyzing project code")
@Validated
public class CodeAnalysisController {

    private static final String SHA_PATTERN = "^[a-f0-9]{7,40}$";

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
    public Map<String, Object> analyzeProject(
            @PathVariable @NotNull(message = "Project ID cannot be null") Long projectId) throws Exception {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project ID must be positive");
        }
        GitProjectEntity project = projectAccessService.getById(projectId);
        File projectDir = new File(project.getLocalPath());
        if (!projectDir.exists()) {
            throw new IllegalArgumentException("Project directory does not exist: " + project.getLocalPath());
        }
        if (!projectDir.isDirectory()) {
            throw new IllegalArgumentException("Project path is not a directory: " + project.getLocalPath());
        }
        return codeAnalysisService.analyzeProject(projectDir);
    }

    @Operation(summary = "Analyze a specific commit",
            description = "Performs static code analysis on the code at a given commit")
    @PostMapping("/project/{projectId}/commit/{commitSha}")
    public Map<String, Object> analyzeCommit(
            @PathVariable @NotNull(message = "Project ID cannot be null") Long projectId,
            @PathVariable @NotBlank(message = "Commit SHA cannot be blank")
            @Pattern(regexp = SHA_PATTERN, message = "Invalid commit SHA format") String commitSha) throws Exception {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project ID must be positive");
        }
        GitProjectEntity project = projectAccessService.getById(projectId);
        File projectDir = new File(project.getLocalPath());
        if (!projectDir.exists()) {
            throw new IllegalArgumentException("Project directory does not exist: " + project.getLocalPath());
        }
        if (!projectDir.isDirectory()) {
            throw new IllegalArgumentException("Project path is not a directory: " + project.getLocalPath());
        }
        return codeAnalysisService.analyzeCommit(projectDir, commitSha);
    }

    @Operation(summary = "Analyze diff between commits",
            description = "Performs analysis on the diff between two commits")
    @PostMapping("/project/{projectId}/diff")
    public Map<String, Object> analyzeDiff(
            @PathVariable @NotNull(message = "Project ID cannot be null") Long projectId,
            @RequestParam @NotBlank(message = "Base commit SHA cannot be blank")
            @Pattern(regexp = SHA_PATTERN, message = "Invalid base commit SHA format") String baseCommit,
            @RequestParam @NotBlank(message = "Target commit SHA cannot be blank")
            @Pattern(regexp = SHA_PATTERN, message = "Invalid target commit SHA format") String targetCommit) throws Exception {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project ID must be positive");
        }
        GitProjectEntity project = projectAccessService.getById(projectId);
        File projectDir = new File(project.getLocalPath());
        if (!projectDir.exists()) {
            throw new IllegalArgumentException("Project directory does not exist: " + project.getLocalPath());
        }
        if (!projectDir.isDirectory()) {
            throw new IllegalArgumentException("Project path is not a directory: " + project.getLocalPath());
        }
        return codeAnalysisService.analyzeDiff(projectDir, baseCommit, targetCommit);
    }
}
