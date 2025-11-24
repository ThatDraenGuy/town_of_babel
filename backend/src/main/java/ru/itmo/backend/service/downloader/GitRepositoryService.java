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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service responsible for cloning Git repositories and managing repositories on disk.
 * All DB operations must be done exclusively via {@link RepositoryAccessService}.
 */
@Service
public class GitRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(GitRepositoryService.class);

    private final GitClient gitClient;
    private final FileManager fileManager;
    private final RepositoryAccessService repositoryAccessService;
    private final Path storagePath;
    private final long expireHours;

    /**
     * Constructs GitRepositoryService.
     *
     * @param gitClient               abstraction of Git operations
     * @param fileManager             abstraction of filesystem operations
     * @param repositoryAccessService service that manages repository metadata and TTL
     * @param storagePath             base directory where repositories are stored
     * @param expireHours             TTL of cached repositories in hours
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
     * Retrieves repository by URL or clones a fresh copy if:
     *  - it does not exist
     *  - or its directory is missing
     *
     * TTL refresh is handled by {@link RepositoryAccessService}.
     *
     * @param repoUrl Git repository URL
     * @return metadata of the stored/updated repository
     */
    public GitRepositoryEntity getOrCloneRepository(String repoUrl) throws GitAPIException, IOException {

        // Look up existing repository and auto-refresh TTL
        Optional<GitRepositoryEntity> existing = repositoryAccessService.accessRepositoryByUrl(repoUrl);

        if (existing.isPresent()) {
            GitRepositoryEntity repo = existing.get();
            File dir = new File(repo.getLocalPath());

            if (dir.exists()) {
                log.info("Using cached repository: {}", repo.getLocalPath());
                return repo;
            }

            // Directory is missing — clean up metadata
            log.warn("Repository directory missing, removing stale metadata: {}", repo.getId());
            repositoryAccessService.delete(repo);
        }

        // Clone a fresh repository
        Path repoDir = storagePath.resolve(UUID.randomUUID().toString());
        Files.createDirectories(repoDir);

        log.info("Cloning repository: {}", repoUrl);

        try {
            gitClient.cloneRepo(repoUrl, repoDir.toFile());
        } catch (Exception ex) {
            log.error("Clone failed for {} — cleaning up directory {}", repoUrl, repoDir, ex);
            fileManager.deleteDirectory(repoDir.toFile());
            throw ex;
        }

        // Save metadata
        GitRepositoryEntity entity = new GitRepositoryEntity();
        entity.setUrl(repoUrl);
        entity.setLocalPath(repoDir.toString());
        entity.setCreatedAt(now());
        entity.setExpiresAt(now().plusHours(expireHours));

        repositoryAccessService.save(entity);

        log.info("Repository cloned and stored: id={} path={}", entity.getId(), entity.getLocalPath());
        return entity;
    }

    /**
     * Performs repository analysis:
     *  - counts total files
     *  - counts Java lines
     *
     * @param repoDir the root of the repository on disk
     * @return map containing analysis results
     */
    public Map<String, Object> analyzeRepository(File repoDir) throws IOException {
        AtomicLong totalFiles = new AtomicLong();
        AtomicLong javaLines = new AtomicLong();

        Files.walk(repoDir.toPath()).forEach(path -> {
            try {
                if (Files.isRegularFile(path)) {
                    totalFiles.incrementAndGet();
                    if (path.toString().endsWith(".java")) {
                        javaLines.addAndGet(Files.lines(path).count());
                    }
                }
            } catch (IOException ignored) { }
        });

        return Map.of(
                "total_files", totalFiles.get(),
                "java_lines", javaLines.get()
        );
    }

    /**
     * Deletes all repositories whose TTL has expired.
     * Uses {@link RepositoryAccessService} for all DB operations.
     */
    public void cleanupExpiredRepositoriesOnce() {
        var expired = repositoryAccessService.findExpired(now());

        for (GitRepositoryEntity repo : expired) {
            log.info("Removing expired repository: {}", repo.getId());
            fileManager.deleteDirectory(new File(repo.getLocalPath()));
            repositoryAccessService.delete(repo);
        }
    }

    /**
     * Scheduled cleanup job, executed every hour.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanupExpiredRepositories() {
        cleanupExpiredRepositoriesOnce();
    }
}