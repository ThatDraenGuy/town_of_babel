package ru.itmo.backend.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.itmo.backend.entity.GitProjectEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GitProjectEntityRepository extends JpaRepository<GitProjectEntity, Long> {

    /**
     * Finds a project by its original Git URL.
     *
     * @param url the Git repository URL
     * @return Optional containing the entity if it exists
     */
    Optional<GitProjectEntity> findByUrl(String url);

    /**
     * Finds a project by its external UUID (projectUuid).
     *
     * @param uuid the Long of the repository
     * @return Optional containing the entity if it exists
     */
    Optional<GitProjectEntity> findById(Long uuid);

    /**
     * Finds all repositories whose TTL has expired.
     *
     * @param now current timestamp
     * @return list of expired repository entities
     */
    @Query("SELECT r FROM GitProjectEntity r WHERE r.expiresAt < :now")
    List<GitProjectEntity> findExpired(@Param("now") LocalDateTime now);
}