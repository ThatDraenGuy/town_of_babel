package ru.itmo.backend.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.stereotype.Service;
import ru.itmo.backend.dto.response.commit.BranchDTO;
import ru.itmo.backend.dto.response.commit.CommitDTO;
import ru.itmo.backend.dto.response.commit.PageResponse;
import ru.itmo.backend.entity.GitProjectEntity;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Service that provides Git-related operations: listing branches and commits with pagination.
 *
 * This implementation:
 * - Returns short branch names to the frontend.
 * - Resolves both local and remote branches when fetching commits.
 * - Supports pagination for branches and commits.
 */
@Service
public class GitCommitService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE = 0;

    /**
     * Lists branches with pagination. Returns short branch names.
     *
     * @param project Git project entity
     * @param page zero-based page index
     * @param pageSize number of items per page
     * @return paginated branch DTOs
     * @throws Exception on Git errors
     */
    public List<BranchDTO> listBranches(GitProjectEntity project) throws Exception {
        Objects.requireNonNull(project, "project must not be null");

        File projectDir = new File(project.getLocalPath());
        if (!projectDir.exists()) {
            throw new IllegalArgumentException("Project directory does not exist: " + project.getLocalPath());
        }
        if (!projectDir.isDirectory()) {
            throw new IllegalArgumentException("Project path is not a directory: " + project.getLocalPath());
        }

        try (Git git = Git.open(projectDir)) {
            // list all branches (local + remote)
            List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

            // map to short names for frontend and remove duplicates
            return refs.stream()
                    .map(ref -> {
                        String fullName = ref.getName();
                        String shortName;
                        if (fullName.startsWith("refs/heads/")) {
                            shortName = fullName.substring("refs/heads/".length());
                        } else if (fullName.startsWith("refs/remotes/origin/")) {
                            shortName = fullName.substring("refs/remotes/origin/".length());
                        } else {
                            shortName = fullName;
                        }
                        String latestSha = ref.getObjectId() != null ? ref.getObjectId().getName() : null;
                        return new BranchDTO(shortName, latestSha);
                    })
                    // remove duplicates (e.g., local master and remote master)
                    .collect(Collectors.toMap(BranchDTO::branchName, b -> b, (b1, b2) -> b1))
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(BranchDTO::branchName))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Gets a specific branch by name.
     */
    public Optional<BranchDTO> getBranch(GitProjectEntity project, String branchName) throws Exception {
        return listBranches(project).stream()
                .filter(b -> b.branchName().equals(branchName))
                .findFirst();
    }

    /**
     * Lists commits of a branch with pagination.
     *
     * Resolves branch from:
     * 1. short name
     * 2. refs/heads/<branch>
     * 3. refs/remotes/origin/<branch>
     *
     * @param project Git project entity
     * @param branch short branch name
     * @param page zero-based page index
     * @param pageSize number of commits per page
     * @return paginated commit DTOs
     * @throws Exception on Git errors
     */
    public PageResponse<CommitDTO> listCommits(GitProjectEntity project, String branch, int page, int pageSize) throws Exception {
        Objects.requireNonNull(project, "project must not be null");
        if (branch == null || branch.isBlank()) throw new IllegalArgumentException("branch must be provided");

        File projectDir = new File(project.getLocalPath());
        if (!projectDir.exists()) {
            throw new IllegalArgumentException("Project directory does not exist: " + project.getLocalPath());
        }
        if (!projectDir.isDirectory()) {
            throw new IllegalArgumentException("Project path is not a directory: " + project.getLocalPath());
        }

        try (Git git = Git.open(projectDir)) {
            var repo = git.getRepository();

            // try resolving short name, local, then remote
            List<String> candidates = List.of(
                    branch,
                    "refs/heads/" + branch,
                    "refs/remotes/origin/" + branch
            );

            ObjectId resolved = null;
            for (String c : candidates) {
                resolved = repo.resolve(c);
                if (resolved != null) break;
            }

            if (resolved == null) {
                throw new IllegalArgumentException("Branch not found: " + branch);
            }

            Iterable<RevCommit> commitsIterable = git.log().add(resolved).call();

            // materialize list for pagination
            List<RevCommit> allCommits = StreamSupport.stream(commitsIterable.spliterator(), false)
                    .toList().reversed();

            int totalCount = allCommits.size();
            List<CommitDTO> allCommitDTOs = new ArrayList<>();
            for (int i = 0; i < totalCount; i++) {
                RevCommit c = allCommits.get(i);
                allCommitDTOs.add(new CommitDTO(
                        c.getName(),
                        c.getFullMessage(),
                        c.getAuthorIdent() != null ? c.getAuthorIdent().getName() : null,
                        ((long) c.getCommitTime()) * 1000L,
                        totalCount - i // Commit number (1-based, descending)
                ));
            }

            return paginate(allCommitDTOs, page, pageSize, CommitDTO.class);
        }
    }

    /**
     * Gets a specific commit by SHA.
     */
    public Optional<CommitDTO> getCommit(GitProjectEntity project, String branch, String sha) throws Exception {
        // This is a simple implementation, might need optimization if many commits
        return listCommits(project, branch, 0, Integer.MAX_VALUE).items().stream()
                .filter(c -> c.sha().equals(sha))
                .findFirst();
    }

    /**
     * Applies pagination to a list of items.
     * Normalizes page and pageSize to default values if invalid.
     *
     * @param <T> type of items in the list
     * @param allItems complete list of items to paginate
     * @param page zero-based page index (normalized to >= 0)
     * @param pageSize number of items per page (normalized to > 0)
     * @param itemType class of items (for type inference)
     * @return paginated response with items for the requested page
     */
    private <T> PageResponse<T> paginate(List<T> allItems, int page, int pageSize, Class<T> itemType) {
        long total = allItems.size();
        
        // Normalize page and pageSize to default values if invalid
        pageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        page = page < 0 ? DEFAULT_PAGE : page;

        int start = (int) Math.min((long) page * pageSize, allItems.size());
        int end = (int) Math.min((long) (page + 1) * pageSize, allItems.size());

        List<T> items = allItems.subList(start, end);

        return new PageResponse<>(items, page, pageSize, total);
    }
}
