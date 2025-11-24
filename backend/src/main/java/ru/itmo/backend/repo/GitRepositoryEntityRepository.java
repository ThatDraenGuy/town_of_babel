package ru.itmo.backend.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.itmo.backend.entity.GitRepositoryEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitRepositoryEntityRepository extends JpaRepository<GitRepositoryEntity, Long> {

    /**
     * Finds a repository by its original Git URL.
     *
     * @param url the Git repository URL
     * @return Optional containing the entity if it exists
     */
    Optional<GitRepositoryEntity> findByUrl(String url);

    /**
     * Finds a repository by its external UUID (repositoryUuid).
     *
     * @param uuid the Long of the repository
     * @return Optional containing the entity if it exists
     */
    Optional<GitRepositoryEntity> findById(Long uuid);

    /**
     * Finds all repositories whose TTL has expired.
     *
     * @param now current timestamp
     * @return list of expired repository entities
     */
    @Query("SELECT r FROM GitRepositoryEntity r WHERE r.expiresAt < :now")
    List<GitRepositoryEntity> findExpired(@Param("now") LocalDateTime now);
}