package ru.itmo.backend.service.downloader;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.itmo.backend.config.metrics.MetricsService;
import ru.itmo.backend.dto.response.gitproject.ProjectResponseDTO;
import ru.itmo.backend.dto.response.gitproject.UpdateStatus;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.entity.ProjectInstanceEntity;
import ru.itmo.backend.exception.*;
import ru.itmo.backend.repo.ProjectInstanceRepository;
import io.micrometer.core.instrument.Timer;

import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for cloning Git projects, updating them via git pull,
 * and managing projects on disk. All DB operations are done exclusively
 * via {@link ProjectAccessService}.
 */
@Service
public class GitProjectService {

    private static final Logger log = LoggerFactory.getLogger(GitProjectService.class);

    private final GitClient gitClient;
    private final FileManager fileManager;
    private final ProjectAccessService projectAccessService;
    private final ProjectInstanceRepository projectInstanceRepository;
    private final MetricsService metricsService;
    private final Path storagePath;
    private final long expireHours;
    private final int instanceCount;
    private final ConcurrentMap<String, ReentrantLock> repoLocks = new ConcurrentHashMap<>();

    private static final Pattern GITHUB_REGEX =
            Pattern.compile("github\\.com[:/](.+?)/(.+?)(\\.git)?$");

    /**
     * Constructs GitProjectService.
     *
     * @param gitClient            abstraction of Git operations
     * @param fileManager          abstraction of filesystem operations
     * @param projectAccessService service that manages project metadata and TTL
     * @param storagePath          base directory where projects are stored
     * @param expireHours          TTL of cached projects in hours
     */
    public GitProjectService(
            GitClient gitClient,
            FileManager fileManager,
            ProjectAccessService projectAccessService,
            ProjectInstanceRepository projectInstanceRepository,
            MetricsService metricsService,
            @Value("${repository.storage.path}") String storagePath,
            @Value("${repository.expire.hours}") long expireHours,
            @Value("${repository.min-free-space-mb:1024}") long minFreeSpaceMb,
            @Value("${repository.instances.count:6}") int instanceCount
    ) {
        this.gitClient = gitClient;
        this.fileManager = fileManager;
        this.projectAccessService = projectAccessService;
        this.projectInstanceRepository = projectInstanceRepository;
        this.metricsService = metricsService;
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
        this.expireHours = expireHours;
        this.instanceCount = instanceCount;

        try {
            Files.createDirectories(this.storagePath);
            
            // Check available disk space
            long freeSpaceBytes = Files.getFileStore(this.storagePath).getUsableSpace();
            long minFreeSpaceBytes = minFreeSpaceMb * 1024 * 1024;
            
            if (freeSpaceBytes < minFreeSpaceBytes) {
                log.warn("Low disk space in storage directory: {} MB available (minimum: {} MB)", 
                        freeSpaceBytes / (1024 * 1024), minFreeSpaceMb);
            } else {
                log.info("Storage directory initialized: {} (free space: {} MB)", 
                        this.storagePath, freeSpaceBytes / (1024 * 1024));
            }
        } catch (IOException e) {
            log.error("Unable to create storage directory {}", storagePath, e);
            throw new IllegalStateException("Failed to initialize storage directory", e);
        }
    }

    /**
     * Returns the current timestamp.
     * Overridable for tests.
     */
    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Retrieves a project by URL, clones it if it does not exist, or updates it
     * if it exists on disk. Returns a DTO with the project information and
     * update status.
     *
     * @param repoUrl Git project URL
     * @return DTO containing project metadata and update status
     * @throws IOException      if filesystem operations fail
     * @throws GitAPIException  if Git operations fail
     */
    @Transactional
    public ProjectResponseDTO getOrCloneProject(String repoUrl) throws IOException, GitAPIException {
        ReentrantLock lock = repoLocks.computeIfAbsent(repoUrl, key -> new ReentrantLock());
        lock.lock();
        try {
            Optional<GitProjectEntity> existingOpt = projectAccessService.accessRepositoryByUrl(repoUrl);
            GitProjectEntity entity;
            UpdateStatus updateStatus;

            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                File dir = new File(entity.getLocalPath());

                if (dir.exists()) {
                    log.info("Repository exists, attempting git pull: {}", entity.getLocalPath());
                    updateStatus = updateProject(entity);
                } else {
                    log.warn("Repository directory missing, removing stale metadata: {}", entity.getId());
                    projectAccessService.delete(entity);
                    entity = cloneNewRepository(repoUrl);
                    updateStatus = UpdateStatus.CLONED;
                }
            } else {
                entity = cloneNewRepository(repoUrl);
                updateStatus = UpdateStatus.CLONED;
            }

            Map<String, String> parsed = parseGithubUrl(repoUrl);

            return new ProjectResponseDTO(
                    entity.getId(),
                    parsed.get("owner"),
                    parsed.get("repo"),
                    entity.getLocalPath(),
                    updateStatus
            );
        } finally {
            lock.unlock();
        }
    }

    /**
     * Performs git pull on an existing repository.
     *
     * @param entity Git project entity
     * @return update status
     */
    private UpdateStatus updateProject(GitProjectEntity entity) {
        File dir = new File(entity.getLocalPath());
        Timer.Sample sample = metricsService.startGitPullTimer();
        boolean success = false;

        try {
            gitClient.pullProject(dir);
            log.info("Main repository updated via git pull: {}", entity.getLocalPath());
            
            // Sync instances
            List<ProjectInstanceEntity> instances = entity.getInstances();
            log.info("Synchronizing {} instances for project {}...", instances.size(), entity.getId());
            for (ProjectInstanceEntity instance : instances) {
                File instanceDir = new File(instance.getLocalPath());
                if (instanceDir.exists()) {
                    gitClient.pullProject(instanceDir);
                    log.info("Synced instance {} (path: {})", instance.getId(), instance.getLocalPath());
                } else {
                    log.warn("Instance directory missing, recreating: {}", instance.getLocalPath());
                    gitClient.cloneLocal(dir, instanceDir);
                }
            }
            
            success = true;
            return UpdateStatus.UPDATED;
        } catch (GitRepositoryNotFoundException e) {
            log.warn("Repository not found during pull (may have been deleted): {} - {}", 
                    entity.getLocalPath(), e.getMessage());
            // Repository was deleted, mark for re-cloning
            return UpdateStatus.NOT_UPDATED;
        } catch (GitNetworkException e) {
            log.error("Network error during git pull for repository {}: {}", 
                     entity.getLocalPath(), e.getMessage());
            return UpdateStatus.NOT_UPDATED;
        } catch (GitConflictException e) {
            log.warn("Merge conflict during git pull for repository {}: {}", 
                    entity.getLocalPath(), e.getMessage());
            return UpdateStatus.NOT_UPDATED;
        } catch (GitAccessException e) {
            log.error("Access denied during git pull for repository {}: {}", 
                     entity.getLocalPath(), e.getMessage());
            return UpdateStatus.NOT_UPDATED;
        } catch (GitOperationException e) {
            log.error("Git operation failed during pull for repository {}: {}", 
                     entity.getLocalPath(), e.getMessage(), e);
            return UpdateStatus.NOT_UPDATED;
        } catch (Exception e) {
            log.error("Unexpected error during git pull for repository {}: {}", 
                     entity.getLocalPath(), e.getMessage(), e);
            return UpdateStatus.NOT_UPDATED;
        } finally {
            metricsService.recordGitPullDuration(sample, success);
        }
    }

    /**
     * Clones a new repository to disk and stores metadata in the database.
     * If database save fails, the cloned directory is cleaned up to prevent disk space leaks.
     *
     * @param repoUrl repository URL
     * @return saved entity
     * @throws IOException     if filesystem operations fail
     * @throws GitAPIException if cloning fails
     */
    private GitProjectEntity cloneNewRepository(String repoUrl) throws IOException, GitAPIException {
        Path projectDir = storagePath.resolve(UUID.randomUUID().toString());
        Files.createDirectories(projectDir);
        boolean directoryCreated = true;
        Timer.Sample sample = metricsService.startCloneTimer();
        boolean success = false;

        log.info("Cloning repository: {}", repoUrl);

        try {
            gitClient.cloneProject(repoUrl, projectDir.toFile());
            
            // Validate that cloned directory is a valid Git repository
            if (!gitClient.isValidGitRepository(projectDir.toFile())) {
                throw new IllegalStateException("Cloned directory is not a valid Git repository: " + projectDir);
            }
            
            success = true;
        } catch (Exception ex) {
            log.error("Clone failed for {} — cleaning up directory {}", repoUrl, projectDir, ex);
            fileManager.deleteDirectory(projectDir.toFile());
            directoryCreated = false;
            throw ex;
        } finally {
            metricsService.recordCloneDuration(sample, success);
        }

        GitProjectEntity entity = new GitProjectEntity();
        entity.setUrl(repoUrl);
        entity.setLocalPath(projectDir.toString());
        entity.setCreatedAt(now());
        entity.setExpiresAt(now().plusHours(expireHours));

        // Create instances
        log.info("Creating {} parallel instances for project {}", instanceCount, repoUrl);
        for (int i = 0; i < instanceCount; i++) {
            Path instancePath = storagePath.resolve(UUID.randomUUID().toString());
            try {
                Files.createDirectories(instancePath);
                gitClient.cloneLocal(projectDir.toFile(), instancePath.toFile());
                
                ProjectInstanceEntity instance = new ProjectInstanceEntity();
                instance.setProject(entity);
                instance.setLocalPath(instancePath.toString());
                instance.setBusy(false);
                entity.getInstances().add(instance);
                log.info("Instance {} created at {}", i + 1, instancePath);
            } catch (Exception e) {
                log.error("Failed to create instance {} for {}: {}", i, repoUrl, e.getMessage());
                // Non-critical, we continue if at least main repo is cloned
            }
        }

        try {
            projectAccessService.save(entity);
            log.info("Repository cloned and stored with {} instances: id={} path={}", 
                    entity.getInstances().size(), entity.getId(), entity.getLocalPath());
            return entity;
        } catch (Exception ex) {
            // Rollback: if database save fails, clean up the cloned directory
            log.error("Failed to save repository metadata to database for {} — cleaning up directory {}", 
                     repoUrl, projectDir, ex);
            if (directoryCreated) {
                try {
                    fileManager.deleteDirectory(projectDir.toFile());
                    log.info("Cleaned up directory after failed database save: {}", projectDir);
                } catch (Exception cleanupEx) {
                    log.error("Failed to clean up directory after database save failure: {}", 
                             projectDir, cleanupEx);
                }
            }
            throw new IllegalStateException("Failed to save repository metadata after cloning", ex);
        }
    }

    /**
     * Parses a GitHub URL into owner and repository name.
     *
     * @param url GitHub repository URL
     * @return map with "owner" and "repo" keys
     */
    public Map<String, String> parseGithubUrl(String url) {
        Matcher matcher = GITHUB_REGEX.matcher(url);
        if (matcher.find()) {
            return Map.of(
                    "owner", matcher.group(1),
                    "repo", matcher.group(2)
            );
        }
        return Map.of(
                "owner", "unknown",
                "repo", "unknown"
        );
    }

    /**
     * Deletes all projects whose TTL has expired.
     */
    @Transactional
    public void cleanupExpiredProjectOnce() {
        var expired = projectAccessService.findExpired(now());

        for (GitProjectEntity project : expired) {
            log.info("Removing expired repository: {}", project.getId());
            
            // Remove main repo
            fileManager.deleteDirectory(new File(project.getLocalPath()));
            
            // Remove all instances
            for (ProjectInstanceEntity instance : project.getInstances()) {
                fileManager.deleteDirectory(new File(instance.getLocalPath()));
            }
            
            projectAccessService.delete(project);
        }
    }

    /**
     * Scheduled cleanup job, executed every hour.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanupExpiredProjects() {
        cleanupExpiredProjectOnce();
    }
}
