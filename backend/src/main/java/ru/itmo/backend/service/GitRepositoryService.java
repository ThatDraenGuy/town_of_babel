package ru.itmo.backend.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
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

    private final GitRepositoryEntityRepository repositoryEntityRepository;
    private final Path storagePath;
    private final long expireHours;

    /**
     * Creates the repository service with configured storage location and expiration timeout.
     *
     * @param repositoryEntityRepository JPA repository for GitRepositoryEntity
     * @param storagePath                base directory where repositories will be stored
     * @param expireHours                TTL of cached repositories in hours
     */
    public GitRepositoryService(
            GitRepositoryEntityRepository repositoryEntityRepository,
            @Value("${repository.storage.path}") String storagePath,
            @Value("${repository.expire.hours}") long expireHours
    ) {
        this.repositoryEntityRepository = repositoryEntityRepository;
        this.storagePath = Path.of(storagePath);
        this.expireHours = expireHours;

        // Ensure the base storage directory exists
        try {
            Files.createDirectories(this.storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create storage directory: " + this.storagePath, e);
        }
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

        // If cached repository exists — check expiration and filesystem validity
        if (existing.isPresent()) {
            GitRepositoryEntity entity = existing.get();
            File localDir = new File(entity.getLocalPath());

            if (localDir.exists() && !isExpired(entity)) {
                System.out.println("Using cached repository: " + entity.getLocalPath());
                return entity;
            } else {
                System.out.println("Cached repository expired or missing. Cleaning up...");
                deleteDirectory(localDir);
                repositoryEntityRepository.delete(entity);
            }
        }

        // Create new local directory for the repository
        Path repoDir = storagePath.resolve(UUID.randomUUID().toString());
        Files.createDirectories(repoDir);

        System.out.println("Cloning repository: " + repoUrl);

        try {
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDir.toFile())
                    .call();
        } catch (Exception e) {
            // Cleanup directory if clone fails
            deleteDirectory(repoDir.toFile());
            throw e;
        }

        // Save new repository metadata
        GitRepositoryEntity entity = new GitRepositoryEntity();
        entity.setUrl(repoUrl);
        entity.setLocalPath(repoDir.toString());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusHours(expireHours));

        repositoryEntityRepository.save(entity);

        System.out.println("Repository cloned and saved: " + entity.getId());
        return entity;
    }

    /**
     * Performs a simple analysis of the repository directory.
     * Currently counts total files and total Java lines.
     * This method can be extended for deeper analysis.
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
     * Checks whether the cached repository entry has expired.
     *
     * @param entity repository metadata entity
     * @return true if expired, false otherwise
     */
    private boolean isExpired(GitRepositoryEntity entity) {
        return LocalDateTime.now().isAfter(entity.getExpiresAt());
    }

    /**
     * Recursively deletes a directory and all nested files.
     *
     * @param directory directory to delete
     */
    private void deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) return;

        try {
            Files.walk(directory.toPath())
                    .sorted((a, b) -> b.compareTo(a))  // delete files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    /**
     * Periodic cleanup task that removes expired repositories
     * both from the filesystem and the database.
     * Runs automatically based on @Scheduled configuration.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // Every 1 hour
    public void cleanupExpiredRepositories() {
        List<GitRepositoryEntity> expiredRepos = repositoryEntityRepository.findAll().stream()
                .filter(this::isExpired)
                .toList();

        for (GitRepositoryEntity entity : expiredRepos) {
            deleteDirectory(new File(entity.getLocalPath()));
            repositoryEntityRepository.delete(entity);
            System.out.println("Deleted expired repository: " + entity.getId());
        }
    }
}