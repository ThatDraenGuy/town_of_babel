package ru.itmo.backend.service.downloader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.itmo.backend.entity.GitRepositoryEntity;
import ru.itmo.backend.repo.GitRepositoryEntityRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service providing safe access to GitRepositoryEntity storage.
 * All DB operations related to Git repositories must go through this service.
 *
 * Responsibilities:
 *  - Find repository by URL or UUID
 *  - Update TTL on access
 *  - Provide list of expired repositories
 *  - Save and delete metadata
 */
@Service
public class RepositoryAccessService {

    private final GitRepositoryEntityRepository repository;
    private final long expireHours;

    /**
     * Constructs a RepositoryAccessService.
     *
     * @param repository the repository interface for GitRepositoryEntity
     * @param expireHours TTL in hours for cached repositories
     */
    public RepositoryAccessService(
            GitRepositoryEntityRepository repository,
            @Value("${repository.expire.hours}") long expireHours
    ) {
        this.repository = repository;
        this.expireHours = expireHours;
    }

    /**
     * Retrieves a repository by its UUID. Refreshes TTL.
     *
     * @param uuid the repository UUID
     * @return repository entity
     */
    public GitRepositoryEntity getById(Long uuid) {
        GitRepositoryEntity entity = repository
                .findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + uuid));

        refreshTTL(entity);
        return entity;
    }

    /**
     * Attempts to find repository by URL and refresh TTL.
     */
    public Optional<GitRepositoryEntity> accessRepositoryByUrl(String url) {
        Optional<GitRepositoryEntity> entity = repository.findByUrl(url);
        entity.ifPresent(this::refreshTTL);
        return entity;
    }

    /**
     * Attempts to find repository by UUID and refresh TTL.
     */
    public Optional<GitRepositoryEntity> accessRepositoryByUuid(Long uuid) {
        Optional<GitRepositoryEntity> entity = repository.findById(uuid);
        entity.ifPresent(this::refreshTTL);
        return entity;
    }

    /**
     * Saves repository metadata.
     */
    public GitRepositoryEntity save(GitRepositoryEntity entity) {
        return repository.save(entity);
    }

    /**
     * Deletes repository metadata.
     */
    public void delete(GitRepositoryEntity entity) {
        repository.delete(entity);
    }

    /**
     * Finds expired repositories (TTL < now).
     */
    public List<GitRepositoryEntity> findExpired(LocalDateTime now) {
        return repository.findExpired(now);
    }

    /**
     * Extends TTL for given repository entity.
     */
    public void refreshTTL(GitRepositoryEntity entity) {
        entity.setExpiresAt(LocalDateTime.now().plusHours(expireHours));
        repository.save(entity);
    }
}
