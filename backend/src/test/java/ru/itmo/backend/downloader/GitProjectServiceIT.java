package ru.itmo.backend.downloader;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.backend.dto.response.gitproject.ProjectResponseDTO;
import ru.itmo.backend.dto.response.gitproject.UpdateStatus;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.repo.GitProjectEntityRepository;
import ru.itmo.backend.service.downloader.GitProjectService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link GitProjectService}.
 */
@SpringBootTest
@ActiveProfiles("h2")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GitProjectServiceIT {

    @Autowired
    private GitProjectService service;

    @Autowired
    private GitProjectEntityRepository repo;

    private Path tempGitRepo;

    @BeforeAll
    void setupGitRepo() throws Exception {
        tempGitRepo = Files.createTempDirectory("local-git-repo-");

        Git.init().setDirectory(tempGitRepo.toFile()).call();
        Files.writeString(tempGitRepo.resolve("TestFile.java"), "class A {}");

        Git.open(tempGitRepo.toFile())
                .add().addFilepattern(".").call();
        Git.open(tempGitRepo.toFile())
                .commit().setMessage("initial commit").call();
    }

    @AfterAll
    void cleanupGitRepo() throws Exception {
        deleteRecursively(tempGitRepo.toFile());
    }

    private void deleteRecursively(File file) {
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursively(f);
                }
            }
            file.delete();
        }
    }

    /**
     * Test: Cloning a repository creates DB entry and filesystem directory.
     */
    @Test
    void testCloneRepositoryIntegration() throws Exception {
        Path repoCopy = Files.createTempDirectory("repo-copy-");
        Git.cloneRepository()
                .setURI(tempGitRepo.toUri().toString())
                .setDirectory(repoCopy.toFile())
                .call();

        String sourceUrl = repoCopy.toUri().toString();
        ProjectResponseDTO dto = service.getOrCloneProject(sourceUrl);

        assertNotNull(dto.projectId(), "Entity ID should be generated");
        assertTrue(new File(dto.path()).exists(), "Repository directory should exist on disk");
        assertEquals(UpdateStatus.CLONED, dto.updateStatus(), "New repository should be marked as CLONED");

        deleteRecursively(new File(dto.path()));
        deleteRecursively(repoCopy.toFile());
    }

    /**
     * Test: Reusing cached repository does not clone again, performs git pull.
     */
    @Test
    void testReuseCachedRepository() throws Exception {
        Path repoCopy = Files.createTempDirectory("repo-copy-");
        Git.cloneRepository()
                .setURI(tempGitRepo.toUri().toString())
                .setDirectory(repoCopy.toFile())
                .call();

        String sourceUrl = repoCopy.toUri().toString();

        ProjectResponseDTO first = service.getOrCloneProject(sourceUrl);
        ProjectResponseDTO second = service.getOrCloneProject(sourceUrl);

        assertEquals(first.projectId(), second.projectId(), "IDs should match for cached repository");
        assertEquals(first.path(), second.path(), "Local paths should match for cached repository");
        assertEquals(UpdateStatus.UPDATED, second.updateStatus(), "Second call should perform git pull");

        deleteRecursively(new File(first.path()));
        deleteRecursively(repoCopy.toFile());
    }

    /**
     * Test: Expired repository is removed from DB and filesystem.
     */
    @Test
    void testExpiredRepositoryCleanup() throws Exception {
        Path repoCopy = Files.createTempDirectory("repo-copy-");
        Git.cloneRepository()
                .setURI(tempGitRepo.toUri().toString())
                .setDirectory(repoCopy.toFile())
                .call();

        String sourceUrl = repoCopy.toUri().toString();
        ProjectResponseDTO dto = service.getOrCloneProject(sourceUrl);

        GitProjectEntity entity = repo.findById(dto.projectId()).orElseThrow();

        // Manually expire repository
        entity.setExpiresAt(LocalDateTime.now().minusDays(1));
        repo.save(entity);

        // Run cleanup
        service.cleanupExpiredProjectOnce();

        assertFalse(repo.findById(entity.getId()).isPresent(), "Expired repository should be removed from DB");
        assertFalse(new File(entity.getLocalPath()).exists(), "Expired repository directory should be deleted");

        deleteRecursively(repoCopy.toFile());
    }
}
