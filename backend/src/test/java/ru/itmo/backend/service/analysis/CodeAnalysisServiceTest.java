package ru.itmo.backend.service.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.itmo.backend.entity.ProjectInstanceEntity;
import ru.itmo.backend.service.ProjectInstanceArbitrator;
import ru.itmo.backend.service.downloader.GitClient;
import ru.itmo.backend.service.reference.ReferenceProperties;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CodeAnalysisServiceTest {

    private ProjectInstanceArbitrator arbitrator;
    private GitClient gitClient;
    private ReferenceProperties props;
    private CodeAnalysisService service;
    private Path tempRepo;

    @BeforeEach
    void setup() throws Exception {
        arbitrator = mock(ProjectInstanceArbitrator.class);
        gitClient = mock(GitClient.class);
        props = mock(ReferenceProperties.class);
        service = new CodeAnalysisService(arbitrator, gitClient, props);
        tempRepo = Files.createTempDirectory("test-repo");
    }

    @Test
    void testAnalyzeProject_AcquiresAndReleasesInstance() throws Exception {
        Long projectId = 1L;
        ProjectInstanceEntity instance = new ProjectInstanceEntity();
        instance.setId(10L);
        instance.setLocalPath(tempRepo.toString());

        when(arbitrator.acquireInstance(projectId)).thenReturn(instance);

        // We need to provide a list of languages now
        String result = service.analyzeProject(projectId, java.util.List.of("Java"));

        assertNotNull(result);
        verify(arbitrator).acquireInstance(projectId);
        verify(arbitrator).releaseInstance(10L);
    }

    @Test
    void testAnalyzeCommit_PerformsCheckout() throws Exception {
        Long projectId = 1L;
        String commitSha = "abc1234";
        ProjectInstanceEntity instance = new ProjectInstanceEntity();
        instance.setId(10L);
        instance.setLocalPath(tempRepo.toString());

        when(arbitrator.acquireInstance(projectId)).thenReturn(instance);

        Map<String, Object> result = service.analyzeCommit(projectId, commitSha);

        assertNotNull(result);
        assertEquals(commitSha, result.get("commit_sha"));
        verify(arbitrator).acquireInstance(projectId);
        verify(gitClient).checkout(any(File.class), eq(commitSha));
        verify(arbitrator).releaseInstance(10L);
    }

    @Test
    void testAnalyzeDiff_AcquiresAndReleasesInstance() throws Exception {
        Long projectId = 1L;
        String oldSha = "abc";
        String newSha = "def";
        ProjectInstanceEntity instance = new ProjectInstanceEntity();
        instance.setId(10L);
        instance.setLocalPath(tempRepo.toString());

        when(arbitrator.acquireInstance(projectId)).thenReturn(instance);

        Map<String, Object> result = service.analyzeDiff(projectId, oldSha, newSha);

        assertNotNull(result);
        verify(arbitrator).acquireInstance(projectId);
        verify(arbitrator).releaseInstance(10L);
    }
}

