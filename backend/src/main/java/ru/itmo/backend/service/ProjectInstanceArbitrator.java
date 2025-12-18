package ru.itmo.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.backend.entity.ProjectInstanceEntity;
import ru.itmo.backend.repo.ProjectInstanceRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ProjectInstanceArbitrator {

    private final ProjectInstanceRepository instanceRepository;

    public ProjectInstanceArbitrator(ProjectInstanceRepository instanceRepository) {
        this.instanceRepository = instanceRepository;
    }

    /**
     * Acquires a free instance for the given project.
     * Uses pessimistic locking via repository to avoid race conditions.
     * 
     * @param projectId project ID
     * @return an available project instance
     * @throws InterruptedException if thread is interrupted while waiting
     * @throws RuntimeException if no instance is available after retries
     */
    @Transactional
    public ProjectInstanceEntity acquireInstance(Long projectId) throws InterruptedException {
        int maxRetries = 30; // Wait up to 30 seconds
        for (int i = 0; i < maxRetries; i++) {
            Optional<ProjectInstanceEntity> available = instanceRepository.findFirstAvailableByProjectId(projectId);
            if (available.isPresent()) {
                ProjectInstanceEntity instance = available.get();
                instance.setBusy(true);
                ProjectInstanceEntity saved = instanceRepository.save(instance);
                log.info("Acquired instance {} (path: {}) for project {}", 
                        saved.getId(), saved.getLocalPath(), projectId);
                return saved;
            }
            
            log.warn("No free instances for project {}, retry {}/{} (waiting for parallel analysis to finish)", 
                    projectId, i + 1, maxRetries);
            TimeUnit.SECONDS.sleep(1);
        }
        
        log.error("Failed to acquire instance for project {} after {} retries", projectId, maxRetries);
        throw new RuntimeException("Timeout waiting for a free project instance for project ID: " + projectId);
    }

    /**
     * Releases a previously acquired instance.
     * 
     * @param instanceId instance ID
     */
    @Transactional
    public void releaseInstance(Long instanceId) {
        instanceRepository.findById(instanceId).ifPresentOrElse(instance -> {
            instance.setBusy(false);
            instance.setLastUsedAt(LocalDateTime.now());
            instanceRepository.save(instance);
            log.info("Released instance {} for project {}. Now available for next tasks.", 
                    instanceId, instance.getProject().getId());
        }, () -> log.error("Attempted to release non-existent instance {}", instanceId));
    }
}

