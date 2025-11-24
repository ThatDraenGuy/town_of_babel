package ru.itmo.backend.service.downloader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.repo.GitProjectEntityRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
public class ProjectAccessService {

    private final GitProjectEntityRepository repository;
    private final long expireHours;

    /**
     * Constructs a RepositoryAccessService.
     *
     * @param repository the repository interface for GitRepositoryEntity
     * @param expireHours TTL in hours for cached repositories
     */
    public ProjectAccessService(
            GitProjectEntityRepository repository,
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
    public GitProjectEntity getById(Long uuid) {
        GitProjectEntity entity = repository
                .findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + uuid));

        refreshTTL(entity);
        return entity;
    }

    /**
     * Attempts to find repository by URL and refresh TTL.
     */
    public Optional<GitProjectEntity> accessRepositoryByUrl(String url) {
        Optional<GitProjectEntity> entity = repository.findByUrl(url);
        entity.ifPresent(this::refreshTTL);
        return entity;
    }

    /**
     * Attempts to find repository by UUID and refresh TTL.
     */
    public Optional<GitProjectEntity> accessRepositoryByUuid(Long uuid) {
        Optional<GitProjectEntity> entity = repository.findById(uuid);
        entity.ifPresent(this::refreshTTL);
        return entity;
    }

    /**
     * Saves repository metadata.
     */
    public GitProjectEntity save(GitProjectEntity entity) {
        return repository.save(entity);
    }

    /**
     * Deletes repository metadata.
     */
    public void delete(GitProjectEntity entity) {
        repository.delete(entity);
    }

    /**
     * Finds expired repositories (TTL < now).
     */
    public List<GitProjectEntity> findExpired(LocalDateTime now) {
        return repository.findExpired(now);
    }

    /**
     * Extends TTL for given repository entity.
     */
    public void refreshTTL(GitProjectEntity entity) {
        entity.setExpiresAt(LocalDateTime.now().plusHours(expireHours));
        repository.save(entity);
    }
}
