package ru.itmo.backend.downloader;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.backend.entity.GitRepositoryEntity;
import ru.itmo.backend.repo.GitRepositoryEntityRepository;
import ru.itmo.backend.service.downloader.GitRepositoryService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link GitRepositoryService}.
 * <p>
 * These tests exercise the end-to-end functionality including:
 * - real Git cloning via JGit,
 * - writing and reading repository entities from the database,
 * - filesystem operations (creating and deleting directories),
 * - expiration and cleanup of cached repositories.
 * </p>
 * <p>
 * The tests use:
 * - H2 in-memory database (via active profile "h2"),
 * - a temporary local Git repository created before all tests.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("h2")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GitRepositoryServiceIT {

    @Autowired
    private GitRepositoryService service;

    @Autowired
    private GitRepositoryEntityRepository repo;

    @Value("${repository.storage.path}")
    private String storagePath;

    private static Path tempGitRepo;

    /**
     * Sets up a temporary Git repository on the local filesystem before all tests.
     * <p>
     * This repository is initialized, a single file is added and committed.
     * Tests will clone from this repository to verify end-to-end behavior.
     * </p>
     *
     * @throws Exception if filesystem or Git operations fail
     */
    @BeforeAll
    static void createLocalGitRepo() throws Exception {
        tempGitRepo = Files.createTempDirectory("local-git-repo-");

        Git.init().setDirectory(tempGitRepo.toFile()).call();

        // create a test Java file
        Files.writeString(tempGitRepo.resolve("TestFile.java"), "class A {}");

        // commit the file
        Git.open(tempGitRepo.toFile())
                .add().addFilepattern(".").call();
        Git.open(tempGitRepo.toFile())
                .commit().setMessage("initial commit").call();

        System.out.println("Created temp git repo at: " + tempGitRepo);
    }

    /**
     * Cleans up the temporary Git repository after all tests.
     * Deletes all files and directories recursively.
     *
     * @throws Exception if filesystem deletion fails
     */
    @AfterAll
    static void cleanup() throws Exception {
        Files.walk(tempGitRepo)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> path.toFile().delete());
    }

    /**
     * Test: Cloning a repository.
     * <p>
     * This test verifies that:
     * 1. The repository is cloned from the local Git URL.
     * 2. A new repository entity is saved in the database.
     * 3. The repository directory is created on the filesystem.
     * </p>
     *
     * @throws Exception if cloning or filesystem operations fail
     */
    @Test
    @Order(1)
    void testCloneRepositoryIntegration() throws Exception {
        String url = tempGitRepo.toUri().toString();

        GitRepositoryEntity entity = service.getOrCloneRepository(url);

        assertNotNull(entity.getId(), "Entity ID should be generated and not null");
        assertEquals(url, entity.getUrl(), "Entity URL should match the cloned URL");
        assertTrue(Files.exists(Path.of(entity.getLocalPath())), "Repository directory should exist on disk");

        System.out.println("Cloned repository to: " + entity.getLocalPath());
    }

    /**
     * Test: Reusing cached repository.
     * <p>
     * This test verifies that:
     * 1. If a repository has already been cloned and is not expired,
     *    getOrCloneRepository returns the same entity.
     * 2. No new cloning occurs on the filesystem.
     * 3. The database entity is reused.
     * </p>
     *
     * @throws Exception if cloning or filesystem operations fail
     */
    @Test
    @Order(2)
    void testReuseCachedRepository() throws Exception {
        String url = tempGitRepo.toUri().toString();

        GitRepositoryEntity first = service.getOrCloneRepository(url);
        GitRepositoryEntity second = service.getOrCloneRepository(url);

        assertEquals(first.getId(), second.getId(), "IDs should match for cached repository");
        assertEquals(first.getLocalPath(), second.getLocalPath(), "Local paths should match for cached repository");
    }

    /**
     * Test: Cleanup of expired repository.
     * <p>
     * This test verifies that:
     * 1. A repository entity manually marked as expired is removed from the database.
     * 2. The corresponding repository directory is deleted from the filesystem.
     * </p>
     *
     * @throws Exception if cloning, database, or filesystem operations fail
     */
    @Test
    @Order(3)
    void testExpiredRepositoryCleanup() throws Exception {
        String url = tempGitRepo.toUri().toString();

        // Ensure repository exists
        GitRepositoryEntity entity = service.getOrCloneRepository(url);

        // Manually expire it
        entity.setExpiresAt(LocalDateTime.now().minusDays(1));
        repo.save(entity);

        // Run cleanup
        service.cleanupExpiredRepositoriesOnce();

        // Verify entity is removed from DB and directory deleted
        assertFalse(repo.findById(entity.getId()).isPresent(), "Expired repository should be removed from database");
        assertFalse(new File(entity.getLocalPath()).exists(), "Expired repository directory should be deleted");
    }
}
