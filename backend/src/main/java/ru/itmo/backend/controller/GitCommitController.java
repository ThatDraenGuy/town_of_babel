package ru.itmo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.response.analysis.CommitMetricsDTO;
import ru.itmo.backend.dto.response.commit.*;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.service.GitCommitService;
import ru.itmo.backend.service.analysis.CodeAnalysisService;
import ru.itmo.backend.service.downloader.ProjectAccessService;

import java.util.List;

/**
 * Controller exposing endpoints to list branches and commits with pagination.
 */
@RestController
@RequestMapping("/projects")
@Tag(name = "git-commit-controller")
public class GitCommitController {

    private final GitCommitService commitService;
    private final ProjectAccessService projectAccessService;
    private final CodeAnalysisService codeAnalysisService;

    public GitCommitController(GitCommitService commitService, ProjectAccessService projectAccessService, CodeAnalysisService codeAnalysisService) {
        this.commitService = commitService;
        this.projectAccessService = projectAccessService;
        this.codeAnalysisService = codeAnalysisService;
    }

    /**
     * Returns a list of branches.
     */
    @Operation(operationId = "getProjectBranches")
    @GetMapping("/{projectId}/branches")
    public ResponseEntity<BranchResponseDTO> getBranches(@PathVariable Long projectId) throws Exception {
        GitProjectEntity project = projectAccessService.getById(projectId);
        List<BranchDTO> branches = commitService.listBranches(project);
        return ResponseEntity.ok(new BranchResponseDTO(branches));
    }

    /**
     * Returns a specific branch.
     */
    @Operation(operationId = "getProjectBranch")
    @GetMapping("/{projectId}/branches/{branch}")
    public ResponseEntity<BranchDTO> getBranch(
            @PathVariable Long projectId,
            @PathVariable String branch
    ) throws Exception {
        GitProjectEntity project = projectAccessService.getById(projectId);
        return commitService.getBranch(project, branch)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns a paginated list of commits for a given branch.
     */
    @Operation(operationId = "getProjectCommits")
    @GetMapping("/{projectId}/branches/{branch}/commits")
    public ResponseEntity<PageResponseCommitDTO> getCommits(
            @PathVariable Long projectId,
            @PathVariable String branch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) throws Exception {
        GitProjectEntity repo = projectAccessService.getById(projectId);
        PageResponse<CommitDTO> response = commitService.listCommits(repo, branch, page, pageSize);
        return ResponseEntity.ok(new PageResponseCommitDTO(
                response.page(),
                response.pageSize(),
                response.total(),
                response.items()
        ));
    }

    /**
     * Returns a specific commit.
     */
    @Operation(operationId = "getProjectCommit")
    @GetMapping("/{projectId}/branches/{branch}/commits/{sha}")
    public ResponseEntity<CommitDTO> getCommit(
            @PathVariable Long projectId,
            @PathVariable String branch,
            @PathVariable String sha
    ) throws Exception {
        GitProjectEntity project = projectAccessService.getById(projectId);
        return commitService.getCommit(project, branch, sha)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns metrics for a specific commit.
     */
    @Operation(operationId = "getCommitMetrics")
    @GetMapping("/{projectId}/branches/{branch}/commits/{sha}/metrics")
    public ResponseEntity<CommitMetricsDTO> getCommitMetrics(
            @PathVariable Long projectId,
            @PathVariable String branch,
            @PathVariable String sha,
            @RequestParam(required = true) List<String> metrics
    ) throws Exception {
        GitProjectEntity project = projectAccessService.getById(projectId);
        CommitDTO commit = commitService.getCommit(project, branch, sha)
                .orElseThrow(() -> new IllegalArgumentException("Commit not found: " + sha));
        
        CommitMetricsDTO metricsDTO = codeAnalysisService.getCommitMetrics(project, commit, metrics);
        return ResponseEntity.ok(metricsDTO);
    }
}
