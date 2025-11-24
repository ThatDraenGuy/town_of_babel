package ru.itmo.backend.downloader;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.backend.entity.GitRepositoryEntity;
import ru.itmo.backend.service.downloader.FileManager;
import ru.itmo.backend.service.downloader.GitClient;
import ru.itmo.backend.service.downloader.GitRepositoryService;
import ru.itmo.backend.service.downloader.RepositoryAccessService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GitRepositoryService}.
 *
 * All external interactions (Git, filesystem, repository access) are mocked
 * to ensure isolated unit tests.
 */
@ActiveProfiles("h2")
public class GitRepositoryServiceTest {

    private GitClient gitClient;
    private FileManager fileManager;
    private RepositoryAccessService accessService;

    private GitRepositoryService service;
    private Path tempStorage;

    private final LocalDateTime FIXED_NOW = LocalDateTime.of(2024, 1, 1, 12, 0);

    /**
     * Custom subclass to override current time for deterministic testing.
     */
    private class TestableGitRepositoryService extends GitRepositoryService {
        public TestableGitRepositoryService() {
            super(gitClient, fileManager, accessService, tempStorage.toString(), 24);
        }

        @Override
        protected LocalDateTime now() {
            return FIXED_NOW;
        }
    }

    @BeforeEach
    void setup() throws IOException {
        gitClient = mock(GitClient.class);
        fileManager = mock(FileManager.class);
        accessService = mock(RepositoryAccessService.class);

        tempStorage = Files.createTempDirectory("git-repo-test-");
        service = new TestableGitRepositoryService();
    }

    /**
     * Test case: When a cached repository exists on disk and is not expired,
     * getOrCloneRepository returns it and does not perform cloning.
     */
    @Test
    void testGetOrCloneRepository_ReturnsCached_WhenNotExpired() throws Exception {
        GitRepositoryEntity entity = new GitRepositoryEntity();
        entity.setUrl("http://repo");
        entity.setLocalPath(tempStorage.resolve("existing").toString());

        // RepositoryAccessService returns existing repository
        when(accessService.accessRepositoryByUrl("http://repo"))
                .thenReturn(Optional.of(entity));

        Files.createDirectories(Path.of(entity.getLocalPath()));

        GitRepositoryEntity result = service.getOrCloneRepository("http://repo");

        assertSame(entity, result);
        verify(gitClient, never()).cloneRepo(any(), any());
        verify(fileManager, never()).deleteDirectory(any());
        verify(accessService).accessRepositoryByUrl("http://repo");
    }

    /**
     * Test case: When cached repository does not exist on disk,
     * getOrCloneRepository clones a new repository and calls refreshTTL.
     */
    @Test
    void testGetOrCloneRepository_Clones_WhenMissing() throws Exception {
        when(accessService.accessRepositoryByUrl("http://repo"))
                .thenReturn(Optional.empty());

        // Мокаем save, чтобы возвращал тот же объект
        when(accessService.save(any(GitRepositoryEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        GitRepositoryEntity result = service.getOrCloneRepository("http://repo");

        // Verify cloning was invoked
        verify(gitClient).cloneRepo(eq("http://repo"), any(File.class));

        // Проверяем, что save вызван с новым репозиторием
        ArgumentCaptor<GitRepositoryEntity> captor = ArgumentCaptor.forClass(GitRepositoryEntity.class);
        verify(accessService).save(captor.capture());

        GitRepositoryEntity saved = captor.getValue();
        assertEquals("http://repo", saved.getUrl());
        assertEquals(FIXED_NOW, saved.getCreatedAt());
        assertEquals(FIXED_NOW.plusHours(24), saved.getExpiresAt());

        assertNotNull(result);
    }

    /**
     * Test case: When cloning fails, getOrCloneRepository deletes the temporary directory
     * and propagates the exception.
     */
    @Test
    void testGetOrCloneRepository_CloneFails_CleansUp() throws Exception {
        when(accessService.accessRepositoryByUrl("http://repo"))
                .thenReturn(Optional.empty());

        doThrow(new GitAPIException("fail") {})
                .when(gitClient)
                .cloneRepo(eq("http://repo"), any(File.class));

        assertThrows(GitAPIException.class, () ->
                service.getOrCloneRepository("http://repo")
        );

        // Verify cleanup was performed
        verify(fileManager).deleteDirectory(any(File.class));
        verify(accessService, never()).refreshTTL(any());
    }

    /**
     * Test case: cleanupExpiredRepositoriesOnce deletes expired repositories from disk
     * and calls refreshTTL for remaining ones.
     */
    @Test
    void testCleanupExpiredRepositoriesOnce_DeletesExpired() throws IOException {
        GitRepositoryEntity expired1 = new GitRepositoryEntity();
        expired1.setLocalPath(tempStorage.resolve("expired1").toString());
        expired1.setExpiresAt(FIXED_NOW.minusHours(1));

        GitRepositoryEntity expired2 = new GitRepositoryEntity();
        expired2.setLocalPath(tempStorage.resolve("expired2").toString());
        expired2.setExpiresAt(FIXED_NOW.minusHours(2));

        when(accessService.findExpired(FIXED_NOW))
                .thenReturn(List.of(expired1, expired2));

        service.cleanupExpiredRepositoriesOnce();

        verify(fileManager).deleteDirectory(new File(expired1.getLocalPath()));
        verify(fileManager).deleteDirectory(new File(expired2.getLocalPath()));

        verify(accessService).delete(expired1);
        verify(accessService).delete(expired2);
    }

}
