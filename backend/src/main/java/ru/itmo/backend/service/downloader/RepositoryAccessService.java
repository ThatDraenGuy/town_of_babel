package ru.itmo.backend.service.downloader;

import org.springframework.stereotype.Service;
import ru.itmo.backend.entity.GitRepositoryEntity;
import ru.itmo.backend.repo.GitRepositoryEntityRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * A service wrapper for accessing Git repositories.
 * Updates the TTL of a repository on every access.
 * Does not perform cloning; it only provides access to existing repositories.
 */
@Service
public class RepositoryAccessService {

    private final GitRepositoryEntityRepository repositoryEntityRepository;
    private final long expireHours;

    /**
     * Constructs a RepositoryAccessService.
     *
     * @param repositoryEntityRepository the repository for GitRepositoryEntity
     * @param expireHours                TTL in hours for cached repositories
     */
    public RepositoryAccessService(GitRepositoryEntityRepository repositoryEntityRepository,
                                   long expireHours) {
        this.repositoryEntityRepository = repositoryEntityRepository;
        this.expireHours = expireHours;
    }

    /**
     * Retrieves an existing repository by its URL and refreshes its TTL.
     *
     * @param repoUrl the repository URL
     * @return Optional containing the repository if it exists
     */
    public Optional<GitRepositoryEntity> accessRepositoryByUrl(String repoUrl) {
        Optional<GitRepositoryEntity> repoOpt = repositoryEntityRepository.findByUrl(repoUrl);
        repoOpt.ifPresent(this::refreshTTL);
        return repoOpt;
    }

    /**
     * Retrieves an existing repository by its UUID and refreshes its TTL.
     *
     * @param uuid the UUID of the repository
     * @return Optional containing the repository if it exists
     */
    public Optional<GitRepositoryEntity> accessRepositoryByUuid(UUID uuid) {
        Optional<GitRepositoryEntity> repoOpt = repositoryEntityRepository.findById(uuid.getMostSignificantBits());
        repoOpt.ifPresent(this::refreshTTL);
        return repoOpt;
    }

    /**
     * Refreshes the TTL of the given repository by extending its expiration.
     *
     * @param repo the Git repository entity
     */
    public void refreshTTL(GitRepositoryEntity repo) {
        repo.setExpiresAt(LocalDateTime.now().plusHours(expireHours));
        repositoryEntityRepository.save(repo);
    }
}
