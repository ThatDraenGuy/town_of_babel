package ru.itmo.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.response.commit.BranchDTO;
import ru.itmo.backend.dto.response.commit.CommitDTO;
import ru.itmo.backend.dto.response.commit.PageResponse;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.service.GitCommitService;
import ru.itmo.backend.service.downloader.ProjectAccessService;

/**
 * Controller exposing endpoints to list branches and commits with pagination.
 */
@RestController
public class GitCommitController {

    private final GitCommitService commitService;
    private final ProjectAccessService projectAccessService;

    public GitCommitController(GitCommitService commitService, ProjectAccessService projectAccessService) {
        this.commitService = commitService;
        this.projectAccessService = projectAccessService;
    }

    /**
     * Returns a paginated list of branches.
     *
     * @param projectId project DB id
     * @param page page index (0-based)
     * @param pageSize page size
     * @return paginated branch DTOs
     * @throws Exception on git errors
     */
    @GetMapping("/{projectId}/branches")
    public ResponseEntity<PageResponse<BranchDTO>> getBranches(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) throws Exception {
        GitProjectEntity project = projectAccessService.getById(projectId);

        PageResponse<BranchDTO> response = commitService.listBranches(project, page, pageSize);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns a paginated list of commits for a given branch.
     *
     * @param projectId project DB id
     * @param branch branch name
     * @param page page index (0-based)
     * @param pageSize page size
     * @return paginated commit DTOs
     * @throws Exception on git errors
     */
    @GetMapping("/{projectId}/branches/{branch}/commits")
    public ResponseEntity<PageResponse<CommitDTO>> getCommits(
            @PathVariable Long projectId,
            @PathVariable String branch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) throws Exception {
        GitProjectEntity repo = projectAccessService.getById(projectId);
        PageResponse<CommitDTO> response = commitService.listCommits(repo, branch, page, pageSize);
        return ResponseEntity.ok(response);
    }
}
