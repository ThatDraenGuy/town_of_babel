package ru.itmo.backend.downloader;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.backend.dto.response.gitproject.ProjectResponseDTO;
import ru.itmo.backend.dto.response.gitproject.UpdateStatus;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.config.metrics.MetricsService;
import ru.itmo.backend.service.downloader.FileManager;
import ru.itmo.backend.service.downloader.GitClient;
import ru.itmo.backend.service.downloader.GitProjectService;
import ru.itmo.backend.service.downloader.ProjectAccessService;
import ru.itmo.backend.repo.ProjectInstanceRepository;
import ru.itmo.backend.service.analysis.CodeAnalysisService;

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
 * Unit tests for {@link GitProjectService}.
 *
 * All external interactions (Git, filesystem, repository access) are mocked
 * to ensure isolated unit tests.
 */
@ActiveProfiles("h2")
public class GitProjectServiceTest {

    private GitClient gitClient;
    private FileManager fileManager;
    private ProjectAccessService accessService;
    private ProjectInstanceRepository instanceRepository;
    private MetricsService metricsService;
    private CodeAnalysisService codeAnalysisService;

    private GitProjectService service;
    private Path tempStorage;

    private final LocalDateTime FIXED_NOW = LocalDateTime.of(2024, 1, 1, 12, 0);

    /**
     * Custom subclass to override current time for deterministic testing.
     */
    private class TestableGitProjectService extends GitProjectService {
        public TestableGitProjectService() {
            super(gitClient, fileManager, accessService, instanceRepository, metricsService, codeAnalysisService, tempStorage.toString(), 24, 1024, 6);
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
        accessService = mock(ProjectAccessService.class);
        instanceRepository = mock(ProjectInstanceRepository.class);
        metricsService = mock(MetricsService.class);
        codeAnalysisService = mock(CodeAnalysisService.class);

        tempStorage = Files.createTempDirectory("git-repo-test-");
        service = new TestableGitProjectService();
    }

    @Test
    void testGetOrCloneProject_ReturnsCachedAndUpdates() throws Exception {
        GitProjectEntity entity = new GitProjectEntity();
        entity.setUrl("http://repo");
        entity.setLocalPath(tempStorage.resolve("existing").toString());
        
        ru.itmo.backend.entity.ProjectInstanceEntity instance = new ru.itmo.backend.entity.ProjectInstanceEntity();
        instance.setLocalPath(tempStorage.resolve("instance1").toString());
        instance.setProject(entity);
        entity.getInstances().add(instance);

        when(accessService.accessRepositoryByUrl("http://repo"))
                .thenReturn(Optional.of(entity));

        Files.createDirectories(Path.of(entity.getLocalPath()));
        Files.createDirectories(Path.of(instance.getLocalPath()));

        ProjectResponseDTO result = service.getOrCloneProject("http://repo");

        assertEquals(entity.getId(), result.projectId());
        assertEquals(UpdateStatus.UPDATED, result.updateStatus());

        verify(gitClient).pullProject(new File(entity.getLocalPath()));
        verify(gitClient).pullProject(new File(instance.getLocalPath()));
        verify(fileManager, never()).deleteDirectory(any());
    }

    @Test
    void testGetOrCloneProject_Clones_WhenMissing() throws Exception {
        when(accessService.accessRepositoryByUrl("http://repo"))
                .thenReturn(Optional.empty());

        when(accessService.save(any(GitProjectEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        when(gitClient.isValidGitRepository(any(File.class)))
                .thenReturn(true);

        ProjectResponseDTO result = service.getOrCloneProject("http://repo");

        assertEquals(UpdateStatus.CLONED, result.updateStatus());

        verify(gitClient).cloneProject(eq("http://repo"), any(File.class));
        verify(gitClient, atLeast(1)).cloneLocal(any(File.class), any(File.class));
        verify(gitClient).isValidGitRepository(any(File.class));

        ArgumentCaptor<GitProjectEntity> captor = ArgumentCaptor.forClass(GitProjectEntity.class);
        verify(accessService).save(captor.capture());

        GitProjectEntity saved = captor.getValue();
        assertEquals("http://repo", saved.getUrl());
        assertEquals(FIXED_NOW, saved.getCreatedAt());
        assertEquals(FIXED_NOW.plusHours(24), saved.getExpiresAt());
        assertEquals(6, saved.getInstances().size());
    }

    @Test
    void testGetOrCloneProject_CloneFails_CleansUp() throws Exception {
        when(accessService.accessRepositoryByUrl("http://repo"))
                .thenReturn(Optional.empty());

        doThrow(new GitAPIException("fail") {})
                .when(gitClient)
                .cloneProject(eq("http://repo"), any(File.class));

        assertThrows(GitAPIException.class, () ->
                service.getOrCloneProject("http://repo")
        );

        verify(fileManager).deleteDirectory(any(File.class));
        verify(gitClient, never()).isValidGitRepository(any(File.class));
    }

    @Test
    void testGetOrCloneProject_InvalidRepository_CleansUp() throws Exception {
        when(accessService.accessRepositoryByUrl("http://repo"))
                .thenReturn(Optional.empty());

        when(gitClient.isValidGitRepository(any(File.class)))
                .thenReturn(false);

        assertThrows(IllegalStateException.class, () ->
                service.getOrCloneProject("http://repo")
        );

        verify(gitClient).cloneProject(eq("http://repo"), any(File.class));
        verify(gitClient).isValidGitRepository(any(File.class));
        verify(fileManager).deleteDirectory(any(File.class));
    }

    @Test
    void testCleanupExpiredProjectOnce_DeletesExpired() throws IOException {
        GitProjectEntity expired1 = new GitProjectEntity();
        expired1.setLocalPath(tempStorage.resolve("expired1").toString());
        expired1.setExpiresAt(FIXED_NOW.minusHours(1));
        
        ru.itmo.backend.entity.ProjectInstanceEntity instance = new ru.itmo.backend.entity.ProjectInstanceEntity();
        instance.setLocalPath(tempStorage.resolve("instance_expired").toString());
        instance.setProject(expired1);
        expired1.getInstances().add(instance);

        GitProjectEntity expired2 = new GitProjectEntity();
        expired2.setLocalPath(tempStorage.resolve("expired2").toString());
        expired2.setExpiresAt(FIXED_NOW.minusHours(2));

        when(accessService.findExpired(FIXED_NOW))
                .thenReturn(List.of(expired1, expired2));

        service.cleanupExpiredProjectOnce();

        verify(fileManager).deleteDirectory(new File(expired1.getLocalPath()));
        verify(fileManager).deleteDirectory(new File(instance.getLocalPath()));
        verify(fileManager).deleteDirectory(new File(expired2.getLocalPath()));

        verify(accessService).delete(expired1);
        verify(accessService).delete(expired2);
    }
}
