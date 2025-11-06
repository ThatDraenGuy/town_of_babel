package ru.itmo.babel.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;
import ru.itmo.babel.entity.GitRepositoryEntity;
import ru.itmo.babel.repo.GitRepositoryEntityRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class GitRepositoryService {
    private final GitRepositoryEntityRepository repositoryEntityRepository;

    public GitRepositoryService(GitRepositoryEntityRepository repositoryEntityRepository) {
        this.repositoryEntityRepository = repositoryEntityRepository;
    }

    public GitRepositoryEntity getOrCloneRepository(String repoUrl) throws GitAPIException, IOException {
        Optional<GitRepositoryEntity> existing = repositoryEntityRepository.findByUrl(repoUrl);
        if (existing.isPresent()) {
            System.out.println("Using cached repo: " + existing.get().getLocalPath());
            return existing.get();
        }

        File tempDir = Files.createTempDirectory("repo-" + UUID.randomUUID()).toFile();
        System.out.println("Cloning repository: " + repoUrl);

        Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(tempDir)
                .call();

        GitRepositoryEntity entity = new GitRepositoryEntity();
        entity.setUrl(repoUrl);
        entity.setLocalPath(tempDir.getAbsolutePath());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(1));

        repositoryEntityRepository.save(entity);

        System.out.println("Repository saved: " + entity.getId());
        return entity;
    }

    public Map<String, Object> analyzeRepository(File repoDir) throws IOException {
        long fileCount = Files.walk(repoDir.toPath())
                .filter(Files::isRegularFile)
                .count();

        long javaLines = Files.walk(repoDir.toPath())
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .mapToLong(p -> {
                    try {
                        return Files.lines(p).count();
                    } catch (IOException e) {
                        return 0;
                    }
                }).sum();

        return Map.of(
                "total_files", fileCount,
                "java_lines", javaLines
        );
    }

}