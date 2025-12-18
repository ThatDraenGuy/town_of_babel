package ru.itmo.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.entity.ProjectInstanceEntity;
import ru.itmo.backend.repo.ProjectInstanceRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProjectInstanceArbitratorTest {

    private ProjectInstanceRepository instanceRepository;
    private ProjectInstanceArbitrator arbitrator;

    @BeforeEach
    void setup() {
        instanceRepository = mock(ProjectInstanceRepository.class);
        arbitrator = new ProjectInstanceArbitrator(instanceRepository);
    }

    private GitProjectEntity createMockProject() {
        GitProjectEntity project = new GitProjectEntity();
        project.setId(1L);
        return project;
    }

    @Test
    void testAcquireInstance_Success() throws InterruptedException {
        Long projectId = 1L;
        ProjectInstanceEntity instance = new ProjectInstanceEntity();
        instance.setId(10L);
        instance.setBusy(false);
        instance.setProject(createMockProject());

        when(instanceRepository.findFirstAvailableByProjectId(projectId))
                .thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(ProjectInstanceEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        ProjectInstanceEntity acquired = arbitrator.acquireInstance(projectId);

        assertNotNull(acquired);
        assertTrue(acquired.isBusy());
        assertEquals(10L, acquired.getId());
        verify(instanceRepository).findFirstAvailableByProjectId(projectId);
        verify(instanceRepository).save(instance);
    }

    @Test
    void testAcquireInstance_RetriesAndFails() {
        Long projectId = 1L;
        when(instanceRepository.findFirstAvailableByProjectId(projectId))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> arbitrator.acquireInstance(projectId));
    }

    @Test
    void testReleaseInstance() {
        Long instanceId = 10L;
        ProjectInstanceEntity instance = new ProjectInstanceEntity();
        instance.setId(instanceId);
        instance.setBusy(true);
        instance.setProject(createMockProject());

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(ProjectInstanceEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        arbitrator.releaseInstance(instanceId);

        assertFalse(instance.isBusy());
        assertNotNull(instance.getLastUsedAt());
        verify(instanceRepository).save(instance);
    }
}

