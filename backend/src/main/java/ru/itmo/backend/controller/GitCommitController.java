package ru.itmo.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.response.BranchDTO;
import ru.itmo.backend.dto.response.CommitDTO;
import ru.itmo.backend.dto.response.PageResponse;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.repo.GitProjectEntityRepository;
import ru.itmo.backend.service.GitCommitService;

/**
 * Controller exposing endpoints to list branches and commits with pagination.
 *
 * Base path: /api/projects
 */
@RestController
@RequestMapping("/api/projects")
public class GitCommitController {

    private final GitProjectEntityRepository projectRepository;
    private final GitCommitService commitService;

    public GitCommitController(GitProjectEntityRepository projectRepository,
                               GitCommitService commitService) {
        this.projectRepository = projectRepository;
        this.commitService = commitService;
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
        GitProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

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
        GitProjectEntity repo = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + projectId));
        PageResponse<CommitDTO> response = commitService.listCommits(repo, branch, page, pageSize);
        return ResponseEntity.ok(response);
    }
}
