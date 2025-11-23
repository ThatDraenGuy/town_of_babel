package ru.itmo.backend.service.downloader;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.itmo.backend.entity.GitRepositoryEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service responsible for cloning and managing Git repositories on disk.
 * Delegates repository access and TTL updates to {@link RepositoryAccessService}.
 */
@Service
public class GitRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(GitRepositoryService.class);

    private final GitClient gitClient;
    private final FileManager fileManager;
    private final Path storagePath;
    private final long expireHours;
    private final RepositoryAccessService repositoryAccessService;

    /**
     * Constructs GitRepositoryService.
     *
     * @param gitClient               abstraction for Git operations
     * @param fileManager             abstraction for filesystem operations
     * @param repositoryAccessService wrapper for repository access and TTL updates
     * @param storagePath             base directory for storing repositories
     * @param expireHours             TTL in hours for cached repositories
     */
    public GitRepositoryService(
            GitClient gitClient,
            FileManager fileManager,
            RepositoryAccessService repositoryAccessService,
            @Value("${repository.storage.path}") String storagePath,
            @Value("${repository.expire.hours}") long expireHours
    ) {
        this.gitClient = gitClient;
        this.fileManager = fileManager;
        this.repositoryAccessService = repositoryAccessService;
        this.storagePath = Path.of(storagePath);
        this.expireHours = expireHours;

        try {
            Files.createDirectories(this.storagePath);
        } catch (IOException e) {
            log.error("Unable to create storage directory: {}", this.storagePath, e);
            throw new RuntimeException("Unable to create storage directory: " + this.storagePath, e);
        }
    }

    /**
     * Returns current time. Can be overridden in tests.
     */
    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Retrieves an existing cached repository by URL or clones a new one if absent or expired.
     * TTL for existing repositories is updated via {@link RepositoryAccessService}.
     *
     * @param repoUrl URL of the Git repository
     * @return GitRepositoryEntity containing metadata about the stored repository
     * @throws GitAPIException if cloning fails
     * @throws IOException     if filesystem operations fail
     */
    public GitRepositoryEntity getOrCloneRepository(String repoUrl) throws GitAPIException, IOException {

        // Check if repository exists and refresh TTL if present
        Optional<GitRepositoryEntity> existing = repositoryAccessService.accessRepositoryByUrl(repoUrl);
        if (existing.isPresent()) {
            GitRepositoryEntity entity = existing.get();
            File localDir = new File(entity.getLocalPath());

            if (localDir.exists()) {
                log.info("Using cached repository: {}", entity.getLocalPath());
                return entity;
            } else {
                log.info("Cached repository missing on disk. Cleaning up database entry: {}", entity.getLocalPath());
                fileManager.deleteDirectory(localDir);
            }
        }

        // Clone new repository
        Path repoDir = storagePath.resolve(UUID.randomUUID().toString());
        Files.createDirectories(repoDir);

        log.info("Cloning repository: {}", repoUrl);
        try {
            gitClient.cloneRepo(repoUrl, repoDir.toFile());
        } catch (Exception e) {
            log.warn("Clone failed for {} — cleaning up directory {}", repoUrl, repoDir, e);
            fileManager.deleteDirectory(repoDir.toFile());
            throw e;
        }

        // Save metadata
        GitRepositoryEntity entity = new GitRepositoryEntity();
        entity.setUrl(repoUrl);
        entity.setLocalPath(repoDir.toString());
        entity.setCreatedAt(now());
        entity.setExpiresAt(now().plusHours(expireHours));

        repositoryAccessService.refreshTTL(entity);
        log.info("Repository cloned and saved: id={}, path={}", entity.getId(), entity.getLocalPath());
        return entity;
    }

    /**
     * Analyzes the repository directory.
     * Counts total files and total Java lines.
     *
     * @param repoDir directory of the repository
     * @return Map containing "total_files" and "java_lines"
     * @throws IOException if reading files fails
     */
    public Map<String, Object> analyzeRepository(File repoDir) throws IOException {
        AtomicLong totalFiles = new AtomicLong(0);
        AtomicLong javaLines = new AtomicLong(0);

        Files.walk(repoDir.toPath()).forEach(path -> {
            try {
                if (Files.isRegularFile(path)) {
                    totalFiles.incrementAndGet();
                    if (path.toString().endsWith(".java")) {
                        javaLines.addAndGet(Files.lines(path).count());
                    }
                }
            } catch (IOException ignored) {
            }
        });

        return Map.of(
                "total_files", totalFiles.get(),
                "java_lines", javaLines.get()
        );
    }

    /**
     * Deletes expired repositories from disk and database.
     * Intended for manual or test invocation.
     */
    public void cleanupExpiredRepositoriesOnce() {
        repositoryAccessService
                .repositoryEntityRepository.findAll().stream()
                .filter(repo -> now().isAfter(repo.getExpiresAt()))
                .forEach(repo -> {
                    fileManager.deleteDirectory(new File(repo.getLocalPath()));
                    repositoryAccessService.repositoryEntityRepository.delete(repo);
                    log.info("Deleted expired repository: {}", repo.getId());
                });
    }

    /**
     * Periodic cleanup task running every hour.
     * Removes expired repositories from filesystem and database.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanupExpiredRepositories() {
        cleanupExpiredRepositoriesOnce();
    }
}
