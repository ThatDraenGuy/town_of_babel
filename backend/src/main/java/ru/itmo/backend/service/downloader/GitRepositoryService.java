package ru.itmo.backend.service.downloader;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.itmo.backend.entity.GitRepositoryEntity;
import ru.itmo.backend.repo.GitRepositoryEntityRepository;

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

@Service
public class GitRepositoryService {
    private static final Logger log = LoggerFactory.getLogger(GitRepositoryService.class);

    private final GitRepositoryEntityRepository repositoryEntityRepository;
    private final Path storagePath;
    private final long expireHours;
    private final GitClient gitClient;
    private final FileManager fileManager;

    /**
     * Creates the repository service with configured storage location and expiration timeout.
     *
     * @param repositoryEntityRepository JPA repository for GitRepositoryEntity
     * @param gitClient                  abstraction of Git operations (mocked in tests)
     * @param fileManager                abstraction over filesystem operations (mocked in tests)
     * @param storagePath                base directory where repositories will be stored
     * @param expireHours                TTL of cached repositories in hours
     */
    public GitRepositoryService(
            GitRepositoryEntityRepository repositoryEntityRepository,
            GitClient gitClient,
            FileManager fileManager,
            @Value("${repository.storage.path}") String storagePath,
            @Value("${repository.expire.hours}") long expireHours
    ) {
        this.repositoryEntityRepository = repositoryEntityRepository;
        this.gitClient = gitClient;
        this.fileManager = fileManager;
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
     * Returns current time. Overridden in tests.
     */
    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Checks whether the cached repository entry has expired.
     *
     * @param entity repository metadata entity
     * @return true if expired, false otherwise
     */
    protected boolean isExpired(GitRepositoryEntity entity) {
        return now().isAfter(entity.getExpiresAt());
    }

    /**
     * Retrieves an existing cached repository or clones a new one if not present or expired.
     *
     * @param repoUrl URL of the Git repository
     * @return GitRepositoryEntity containing metadata about the stored repository
     * @throws GitAPIException if cloning fails
     * @throws IOException     if filesystem operations fail
     */
    public GitRepositoryEntity getOrCloneRepository(String repoUrl) throws GitAPIException, IOException {

        Optional<GitRepositoryEntity> existing = repositoryEntityRepository.findByUrl(repoUrl);

        if (existing.isPresent()) {
            GitRepositoryEntity entity = existing.get();
            File localDir = new File(entity.getLocalPath());

            if (localDir.exists() && !isExpired(entity)) {
                log.info("Using cached repository: {}", entity.getLocalPath());
                return entity;
            } else {
                log.info("Cached repository expired or missing. Cleaning up... (path={})", entity.getLocalPath());
                System.out.println();
                fileManager.deleteDirectory(localDir);
                repositoryEntityRepository.delete(entity);
            }
        }

        Path repoDir = storagePath.resolve(UUID.randomUUID().toString());
        Files.createDirectories(repoDir);

        log.info("Cloning repository: {}" ,repoUrl);

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

        repositoryEntityRepository.save(entity);

        System.out.println();
        log.info("Repository cloned and saved: id={}, path={}", entity.getId(), entity.getLocalPath());
        return entity;
    }

    /**
     * Performs a simple analysis of the repository directory.
     * Counts total files and total Java lines.
     *
     * @param repoDir directory of the repository
     * @return Map-like structure with analysis metrics
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
     * Performs a single cleanup pass: deletes expired repositories.
     * Intended for direct invocation in tests.
     */
    public void cleanupExpiredRepositoriesOnce() {
        List<GitRepositoryEntity> expiredRepos = repositoryEntityRepository.findAll().stream()
                .filter(this::isExpired)
                .toList();

        for (GitRepositoryEntity entity : expiredRepos) {
            fileManager.deleteDirectory(new File(entity.getLocalPath()));
            repositoryEntityRepository.delete(entity);
            log.info("Deleted expired repository: {}", entity.getId());
        }
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
