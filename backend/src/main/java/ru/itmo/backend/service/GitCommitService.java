package ru.itmo.backend.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.itmo.backend.dto.response.BranchDTO;
import ru.itmo.backend.dto.response.CommitDTO;
import ru.itmo.backend.dto.response.PageResponse;
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

    private static final Logger log = LoggerFactory.getLogger(GitCommitService.class);

    /**
     * Lists branches with pagination. Returns short branch names.
     *
     * @param project Git project entity
     * @param page zero-based page index
     * @param pageSize number of items per page
     * @return paginated branch DTOs
     * @throws Exception on Git errors
     */
    public PageResponse<BranchDTO> listBranches(GitProjectEntity project, int page, int pageSize) throws Exception {
        Objects.requireNonNull(project, "project must not be null");

        try (Git git = Git.open(new File(project.getLocalPath()))) {
            // list all branches (local + remote)
            List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

            // map to short names for frontend and remove duplicates
            List<BranchDTO> allBranches = refs.stream()
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
                    .collect(Collectors.toMap(BranchDTO::name, b -> b, (b1, b2) -> b1))
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(BranchDTO::name))
                    .collect(Collectors.toList());

            long total = allBranches.size();
            pageSize = pageSize <= 0 ? 20 : pageSize;
            page = page < 0 ? 0 : page;

            int start = (int) Math.min((long) page * pageSize, allBranches.size());
            int end = (int) Math.min((long) (page + 1) * pageSize, allBranches.size());

            List<BranchDTO> items = allBranches.subList(start, end);

            return new PageResponse<>(items, page, pageSize, total);
        }
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

        try (Git git = Git.open(new File(project.getLocalPath()))) {
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
                    .toList();

            long total = allCommits.size();
            pageSize = pageSize <= 0 ? 20 : pageSize;
            page = page < 0 ? 0 : page;

            int start = (int) Math.min((long) page * pageSize, allCommits.size());
            int end = (int) Math.min((long) (page + 1) * pageSize, allCommits.size());

            List<CommitDTO> items = allCommits.subList(start, end).stream()
                    .map(c -> new CommitDTO(
                            c.getName(),
                            c.getFullMessage(),
                            c.getAuthorIdent() != null ? c.getAuthorIdent().getName() : null,
                            ((long) c.getCommitTime()) * 1000L
                    ))
                    .collect(Collectors.toList());

            return new PageResponse<>(items, page, pageSize, total);
        }
    }
}
