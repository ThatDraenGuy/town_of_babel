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
 * ---
 * The test uses:
 * - real JGit cloning
 * - real filesystem
 * - in-memory H2 database
 * - a temporary local git repository created before the test
 * ---
 * This verifies the complete end-to-end behavior without mocks.
 */
@SpringBootTest
@ActiveProfiles("test")
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
     * Creates a real temporary git repository for integration tests.
     */
    @BeforeAll
    static void createLocalGitRepo() throws Exception {
        tempGitRepo = Files.createTempDirectory("local-git-repo-");

        Git.init().setDirectory(tempGitRepo.toFile()).call();

        // create a test file
        Files.writeString(tempGitRepo.resolve("TestFile.java"), "class A {}");

        // commit file
        Git.open(tempGitRepo.toFile())
                .add().addFilepattern(".").call();

        Git.open(tempGitRepo.toFile())
                .commit().setMessage("initial commit").call();

        System.out.println("Created temp git repo at: " + tempGitRepo);
    }

    /**
     * Cleans up filesystem after tests.
     */
    @AfterAll
    static void cleanup() throws Exception {
        Files.walk(tempGitRepo)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> path.toFile().delete());
    }

    /**
     * Test: service clones repository, writes entity to DB,
     * and creates the directory on filesystem.
     */
    @Test
    @Order(1)
    void testCloneRepositoryIntegration() throws Exception {
        String url = tempGitRepo.toUri().toString();

        GitRepositoryEntity entity = service.getOrCloneRepository(url);

        assertNotNull(entity.getId());
        assertEquals(url, entity.getUrl());
        assertTrue(Files.exists(Path.of(entity.getLocalPath())));

        System.out.println("Cloned to: " + entity.getLocalPath());
    }

    /**
     * Test: cached repo exists and is NOT expired → reused.
     */
    @Test
    @Order(2)
    void testReuseCachedRepository() throws Exception {
        String url = tempGitRepo.toUri().toString();
        GitRepositoryEntity first = service.getOrCloneRepository(url);
        GitRepositoryEntity second = service.getOrCloneRepository(url);

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getLocalPath(), second.getLocalPath());
    }

    /**
     * Test: mark cached repo as expired → cleanup removes it.
     */
    @Test
    @Order(3)
    void testExpiredRepositoryCleanup() throws Exception {
        String url = tempGitRepo.toUri().toString();

        // get entity
        GitRepositoryEntity entity = service.getOrCloneRepository(url);

        // expire it manually
        entity.setExpiresAt(LocalDateTime.now().minusDays(1));
        repo.save(entity);

        // cleanup
        service.cleanupExpiredRepositoriesOnce();

        assertFalse(repo.findById(entity.getId()).isPresent());
        assertFalse(new File(entity.getLocalPath()).exists());
    }
}