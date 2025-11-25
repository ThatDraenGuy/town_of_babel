package ru.itmo.backend.service.downloader;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.itmo.backend.dto.response.ProjectResponseDTO;
import ru.itmo.backend.entity.GitProjectEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for cloning Git projects and managing projects on disk.
 * All DB operations must be done exclusively via {@link ProjectAccessService}.
 */
@Service
public class GitProjectService {

    private static final Logger log = LoggerFactory.getLogger(GitProjectService.class);

    private final GitClient gitClient;
    private final FileManager fileManager;
    private final ProjectAccessService projectAccessService;
    private final Path storagePath;
    private final long expireHours;

    private static final Pattern GITHUB_REGEX =
            Pattern.compile("github\\.com[:/](.+?)/(.+?)(\\.git)?$");
    /**
     * Constructs GitRepositoryService.
     *
     * @param gitClient               abstraction of Git operations
     * @param fileManager             abstraction of filesystem operations
     * @param projectAccessService    service that manages project metadata and TTL
     * @param storagePath             base directory where projects are stored
     * @param expireHours             TTL of cached projects in hours
     */
    public GitProjectService(
            GitClient gitClient,
            FileManager fileManager,
            ProjectAccessService projectAccessService,
            @Value("${repository.storage.path}") String storagePath,
            @Value("${repository.expire.hours}") long expireHours
    ) {
        this.gitClient = gitClient;
        this.fileManager = fileManager;
        this.projectAccessService = projectAccessService;
        this.storagePath = Path.of(storagePath);
        this.expireHours = expireHours;

        try {
            Files.createDirectories(this.storagePath);
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
     * Retrieves projects by URL or clones a fresh copy if:
     *  - it does not exist
     *  - or its directory is missing
     *
     * TTL refresh is handled by {@link ProjectAccessService}.
     *
     * @param repoUrl Git project URL
     * @return metadata of the stored/updated repository
     */
    public ProjectResponseDTO cloneOrGetProject(String repoUrl) throws Exception {
        GitProjectEntity entity = getOrCloneProject(repoUrl);
        Map<String, String> parsed = parseGithubUrl(repoUrl);

        return new ProjectResponseDTO(
                entity.getId(),
                parsed.get("owner"),
                parsed.get("repo"),
                entity.getLocalPath()
        );
    }


    public GitProjectEntity getOrCloneProject(String repoUrl) throws GitAPIException, IOException {

        // Look up existing project and auto-refresh TTL
        Optional<GitProjectEntity> existing = projectAccessService.accessRepositoryByUrl(repoUrl);

        if (existing.isPresent()) {
            GitProjectEntity project = existing.get();
            File dir = new File(project.getLocalPath());

            if (dir.exists()) {
                log.info("Using cached repository: {}", project.getLocalPath());
                return project;
            }

            // Directory is missing — clean up metadata
            log.warn("Repository directory missing, removing stale metadata: {}", project.getId());
            projectAccessService.delete(project);
        }

        // Clone a fresh repository
        Path projectDir = storagePath.resolve(UUID.randomUUID().toString());
        Files.createDirectories(projectDir);

        log.info("Cloning project: {}", repoUrl);

        try {
            gitClient.cloneProject(repoUrl, projectDir.toFile());
        } catch (Exception ex) {
            log.error("Clone failed for {} — cleaning up directory {}", repoUrl, projectDir, ex);
            fileManager.deleteDirectory(projectDir.toFile());
            throw ex;
        }

        // Save metadata
        GitProjectEntity entity = new GitProjectEntity();
        entity.setUrl(repoUrl);
        entity.setLocalPath(projectDir.toString());
        entity.setCreatedAt(now());
        entity.setExpiresAt(now().plusHours(expireHours));

        projectAccessService.save(entity);

        log.info("Project cloned and stored: id={} path={}", entity.getId(), entity.getLocalPath());
        return entity;
    }

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
     * Uses {@link ProjectAccessService} for all DB operations.
     */
    public void cleanupExpiredProjectOnce() {
        var expired = projectAccessService.findExpired(now());

        for (GitProjectEntity project : expired) {
            log.info("Removing expired repository: {}", project.getId());
            fileManager.deleteDirectory(new File(project.getLocalPath()));
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