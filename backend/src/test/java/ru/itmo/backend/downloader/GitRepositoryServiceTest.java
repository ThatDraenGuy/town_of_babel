package ru.itmo.backend.downloader;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.backend.entity.GitRepositoryEntity;
import ru.itmo.backend.repo.GitRepositoryEntityRepository;
import ru.itmo.backend.service.downloader.FileManager;
import ru.itmo.backend.service.downloader.GitClient;
import ru.itmo.backend.service.downloader.GitRepositoryService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GitRepositoryService}.
 *
 * All external interactions (Git, filesystem, database)
 * are mocked to ensure pure unit-test behavior.
 */
@ActiveProfiles("h2")
public class GitRepositoryServiceTest {

    private GitRepositoryEntityRepository repo;
    private GitClient gitClient;
    private FileManager fileManager;

    private GitRepositoryService service;

    private Path tempStorage;

    private final LocalDateTime FIXED_NOW = LocalDateTime.of(2024, 1, 1, 12, 0);

    /**
     * Custom subclass to override time in tests.
     */
    private class TestableGitRepositoryService extends GitRepositoryService {
        public TestableGitRepositoryService() {
            super(repo, gitClient, fileManager, tempStorage.toString(), 24);
        }

        @Override
        protected LocalDateTime now() {
            return FIXED_NOW;
        }
    }

    @BeforeEach
    void setup() throws IOException {
        repo = mock(GitRepositoryEntityRepository.class);
        gitClient = mock(GitClient.class);
        fileManager = mock(FileManager.class);

        tempStorage = Files.createTempDirectory("git-repo-test-");
        service = new TestableGitRepositoryService();
    }

    /**
     * Test: if cached repo exists and is not expired → return it, do not clone.
     */
    @Test
    void testGetOrCloneRepository_ReturnsCached_WhenNotExpired() throws Exception {
        GitRepositoryEntity entity = new GitRepositoryEntity();
        entity.setId(1L);
        entity.setUrl("http://repo");
        entity.setLocalPath(tempStorage.resolve("old").toString());
        entity.setExpiresAt(FIXED_NOW.plusHours(1));

        when(repo.findByUrl("http://repo")).thenReturn(Optional.of(entity));

        Files.createDirectories(Path.of(entity.getLocalPath()));

        GitRepositoryEntity result = service.getOrCloneRepository("http://repo");

        assertSame(entity, result);
        verify(gitClient, never()).cloneRepo(any(), any());
        verify(repo, never()).delete(any());
    }

    /**
     * Test: cached repo exists but is expired → delete directory, delete DB record,
     * clone new one, save new entity.
     */
    @Test
    void testGetOrCloneRepository_Clones_WhenExpired() throws Exception {
        GitRepositoryEntity expired = new GitRepositoryEntity();
        expired.setId(2L);
        expired.setUrl("http://repo");
        expired.setLocalPath(tempStorage.resolve("expired").toString());
        expired.setExpiresAt(FIXED_NOW.minusHours(1)); // expired

        when(repo.findByUrl("http://repo")).thenReturn(Optional.of(expired));

        Files.createDirectories(Path.of(expired.getLocalPath()));

        // Capture saved entity
        ArgumentCaptor<GitRepositoryEntity> captor = ArgumentCaptor.forClass(GitRepositoryEntity.class);

        GitRepositoryEntity result = service.getOrCloneRepository("http://repo");

        // Verify deletion
        verify(fileManager).deleteDirectory(new File(expired.getLocalPath()));
        verify(repo).delete(expired);

        // Verify cloning
        verify(gitClient).cloneRepo(eq("http://repo"), any(File.class));

        // Verify saving new entity
        verify(repo).save(captor.capture());
        GitRepositoryEntity saved = captor.getValue();

        assertEquals("http://repo", saved.getUrl());
        assertEquals(FIXED_NOW, saved.getCreatedAt());
        assertEquals(FIXED_NOW.plusHours(24), saved.getExpiresAt());

        assertNotNull(result);
    }

    /**
     * Test: when cloning fails, the directory must be removed and exception rethrown.
     */
    @Test
    void testGetOrCloneRepository_CloneFails_CleansUp() throws Exception {
        when(repo.findByUrl("http://repo")).thenReturn(Optional.empty());

        doThrow(new GitAPIException("fail") {})
                .when(gitClient)
                .cloneRepo(eq("http://repo"), any(File.class));

        assertThrows(GitAPIException.class, () ->
                service.getOrCloneRepository("http://repo")
        );

        verify(fileManager).deleteDirectory(any(File.class));
        verify(repo, never()).save(any());
    }

    /**
     * Test cleanupExpiredRepositoriesOnce: deletes expired entities.
     */
    @Test
    void testCleanupExpiredRepositoriesOnce() {
        GitRepositoryEntity expired1 = new GitRepositoryEntity();
        expired1.setId(1L);
        expired1.setLocalPath("/tmp/a");
        expired1.setExpiresAt(FIXED_NOW.minusHours(5));

        GitRepositoryEntity expired2 = new GitRepositoryEntity();
        expired2.setId(2L);
        expired2.setLocalPath("/tmp/b");
        expired2.setExpiresAt(FIXED_NOW.minusHours(10));

        when(repo.findAll()).thenReturn(java.util.List.of(expired1, expired2));

        service.cleanupExpiredRepositoriesOnce();

        verify(fileManager).deleteDirectory(new File("/tmp/a"));
        verify(fileManager).deleteDirectory(new File("/tmp/b"));

        verify(repo).delete(expired1);
        verify(repo).delete(expired2);
    }
}