package ru.itmo.backend.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.lib.Ref;
import org.springframework.stereotype.Service;
import ru.itmo.backend.entity.GitRepositoryEntity;
import ru.itmo.backend.dto.response.BranchDTO;
import ru.itmo.backend.dto.response.CommitDTO;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class GitCommitService {

    /**
     * Lists branches of a repository with pagination.
     *
     * @param repo     repository entity
     * @param page     page number (0-based)
     * @param pageSize page size
     * @return list of branch DTOs
     * @throws Exception if Git operations fail
     */
    public List<BranchDTO> listBranches(GitRepositoryEntity repo, int page, int pageSize) throws Exception {
        try (Git git = Git.open(new File(repo.getLocalPath()))) {
            List<Ref> refs = git.branchList().call();
            return refs.stream()
                    .skip((long) page * pageSize)
                    .limit(pageSize)
                    .map(ref -> {
                        String branchName = ref.getName().replace("refs/heads/", "");
                        String latestSha = ref.getObjectId() != null ? ref.getObjectId().getName() : null;
                        return new BranchDTO(branchName, latestSha);
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * Lists commits of a specific branch with pagination.
     *
     * @param repo     repository entity
     * @param branch   branch name
     * @param page     page number (0-based)
     * @param pageSize page size
     * @return list of commit DTOs
     * @throws Exception if Git operations fail
     */
    public List<CommitDTO> listCommits(GitRepositoryEntity repo, String branch, int page, int pageSize) throws Exception {
        try (Git git = Git.open(new File(repo.getLocalPath()))) {
            Iterable<RevCommit> commitsIterable = git.log().add(git.getRepository().resolve(branch)).call();

            return StreamSupport.stream(commitsIterable.spliterator(), false)
                    .skip((long) page * pageSize)
                    .limit(pageSize)
                    .map(c -> new CommitDTO(
                            c.getName(),
                            c.getFullMessage(),
                            c.getAuthorIdent().getName(),
                            c.getCommitTime() * 1000L
                    ))
                    .collect(Collectors.toList());
        }
    }
}
