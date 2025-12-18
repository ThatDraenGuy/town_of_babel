package ru.itmo.backend.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.itmo.backend.entity.ProjectInstanceEntity;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface ProjectInstanceRepository extends JpaRepository<ProjectInstanceEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pi FROM ProjectInstanceEntity pi WHERE pi.project.id = :projectId AND pi.isBusy = false ORDER BY pi.lastUsedAt ASC LIMIT 1")
    Optional<ProjectInstanceEntity> findFirstAvailableByProjectId(@Param("projectId") Long projectId);
}

