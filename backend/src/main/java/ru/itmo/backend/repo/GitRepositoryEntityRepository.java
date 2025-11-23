package ru.itmo.backend.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.itmo.backend.entity.GitRepositoryEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GitRepositoryEntityRepository extends JpaRepository<GitRepositoryEntity, Long> {

    Optional<GitRepositoryEntity> findByUrl(String url);

    @Query("SELECT r FROM GitRepositoryEntity r WHERE r.expiresAt < :now")
    List<GitRepositoryEntity> findExpired(@Param("now") LocalDateTime now);
}
