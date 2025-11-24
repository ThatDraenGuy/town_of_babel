package ru.itmo.backend.downloader;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
 * Each test is fully isolated and does not depend on the others.
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

    /**
     * Create a bare temporary Git repository before all tests.
     */
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

    /**
     * Cleans up the temporary Git repository after all tests.
     */
    @AfterAll
    void cleanupGitRepo() throws Exception {
        deleteRecursively(tempGitRepo.toFile());
    }

    /**
     * Deletes a directory recursively.
     */
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
     * Test: Cloning a repository creates a DB entry and filesystem directory.
     */
    @Test
    void testCloneRepositoryIntegration() throws Exception {
        // Make a fresh copy of tempGitRepo for this test
        Path repoCopy = Files.createTempDirectory("repo-copy-");
        Git.cloneRepository()
                .setURI(tempGitRepo.toUri().toString())
                .setDirectory(repoCopy.toFile())
                .call(); // just to simulate a real repo path

        String url = repoCopy.toUri().toString();

        GitProjectEntity entity = service.getOrCloneProject(url);

        assertNotNull(entity.getId(), "Entity ID should be generated and not null");
        assertEquals(url, entity.getUrl(), "Entity URL should match the cloned URL");
        assertTrue(new File(entity.getLocalPath()).exists(), "Repository directory should exist on disk");

        deleteRecursively(new File(entity.getLocalPath()));
        deleteRecursively(repoCopy.toFile());
    }

    /**
     * Test: Reusing cached repository does not clone again.
     */
    @Test
    void testReuseCachedRepository() throws Exception {
        Path repoCopy = Files.createTempDirectory("repo-copy-");
        Git.cloneRepository()
                .setURI(tempGitRepo.toUri().toString())
                .setDirectory(repoCopy.toFile())
                .call();

        String url = repoCopy.toUri().toString();

        GitProjectEntity first = service.getOrCloneProject(url);
        GitProjectEntity second = service.getOrCloneProject(url);

        assertEquals(first.getId(), second.getId(), "IDs should match for cached repository");
        assertEquals(first.getLocalPath(), second.getLocalPath(), "Local paths should match for cached repository");

        deleteRecursively(new File(first.getLocalPath()));
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

        String url = repoCopy.toUri().toString();

        GitProjectEntity entity = service.getOrCloneProject(url);

        // Manually expire repository
        entity.setExpiresAt(LocalDateTime.now().minusDays(1));
        repo.save(entity);

        // Run cleanup
        service.cleanupExpiredProjectOnce();

        assertFalse(repo.findById(entity.getId()).isPresent(), "Expired repository should be removed from database");
        assertFalse(new File(entity.getLocalPath()).exists(), "Expired repository directory should be deleted");

        deleteRecursively(repoCopy.toFile());
    }
}
