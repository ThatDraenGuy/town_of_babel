package ru.itmo.backend.config.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryStorageHealthIndicatorTest {

    @TempDir
    Path tempDir;

    private RepositoryStorageHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new RepositoryStorageHealthIndicator(tempDir.toString());
    }

    @Test
    void testHealth_WhenDirectoryExists_ReturnsUp() {
        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("path"));
        assertTrue(health.getDetails().containsKey("freeSpace"));
        assertTrue(health.getDetails().containsKey("totalSpace"));
        assertTrue(health.getDetails().containsKey("freeSpacePercent"));
    }

    @Test
    void testHealth_WhenDirectoryDoesNotExist_ReturnsDown() throws Exception {
        Path nonExistentPath = tempDir.resolve("non-existent");
        healthIndicator = new RepositoryStorageHealthIndicator(nonExistentPath.toString());

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Storage directory does not exist", health.getDetails().get("reason"));
    }

    @Test
    void testHealth_WhenPathIsFile_ReturnsDown() throws Exception {
        Path filePath = tempDir.resolve("file.txt");
        Files.createFile(filePath);
        healthIndicator = new RepositoryStorageHealthIndicator(filePath.toString());

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Storage path is not a directory", health.getDetails().get("reason"));
    }

    @Test
    void testHealth_ContainsStorageDetails() {
        Health health = healthIndicator.health();

        assertTrue(health.getDetails().containsKey("path"));
        assertTrue(health.getDetails().containsKey("freeSpace"));
        assertTrue(health.getDetails().containsKey("totalSpace"));
        assertTrue(health.getDetails().containsKey("freeSpacePercent"));
        
        String freeSpacePercent = (String) health.getDetails().get("freeSpacePercent");
        assertNotNull(freeSpacePercent);
        assertTrue(freeSpacePercent.endsWith("%"));
    }
}

