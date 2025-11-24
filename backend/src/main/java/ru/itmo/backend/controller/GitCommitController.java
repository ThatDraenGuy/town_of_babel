package ru.itmo.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.entity.GitRepositoryEntity;
import ru.itmo.backend.repo.GitRepositoryEntityRepository;
import ru.itmo.backend.service.GitCommitService;
import ru.itmo.backend.dto.response.BranchDTO;
import ru.itmo.backend.dto.response.CommitDTO;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
public class GitCommitController {

    private final GitRepositoryEntityRepository repoRepository;
    private final GitCommitService commitService;

    @Autowired
    public GitCommitController(GitRepositoryEntityRepository repoRepository,
                               GitCommitService commitService) {
        this.repoRepository = repoRepository;
        this.commitService = commitService;
    }

    /**
     * Returns a paginated list of branches for a repository.
     */
    @GetMapping("/{repoId}/branches")
    public List<BranchDTO> getBranches(
            @PathVariable Long repoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) throws Exception {
        GitRepositoryEntity repo = repoRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repoId));
        return commitService.listBranches(repo, page, pageSize);
    }

    /**
     * Returns a paginated list of commits for a branch.
     */
    @GetMapping("/{repoId}/branches/{branch}/commits")
    public List<CommitDTO> getCommits(
            @PathVariable Long repoId,
            @PathVariable String branch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) throws Exception {
        GitRepositoryEntity repo = repoRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repoId));
        return commitService.listCommits(repo, branch, page, pageSize);
    }
}