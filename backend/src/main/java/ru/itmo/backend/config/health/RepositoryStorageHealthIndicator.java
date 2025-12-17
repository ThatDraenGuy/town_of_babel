package ru.itmo.backend.config.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Health indicator for repository storage.
 * Checks if the storage directory exists and is writable.
 */
@Component
public class RepositoryStorageHealthIndicator implements HealthIndicator {

    private final Path storagePath;

    public RepositoryStorageHealthIndicator(@Value("${repository.storage.path}") String storagePath) {
        this.storagePath = Path.of(storagePath);
    }

    @Override
    public Health health() {
        try {
            File storageDir = storagePath.toFile();
            
            if (!storageDir.exists()) {
                return Health.down()
                        .withDetail("path", storagePath.toString())
                        .withDetail("reason", "Storage directory does not exist")
                        .build();
            }

            if (!storageDir.isDirectory()) {
                return Health.down()
                        .withDetail("path", storagePath.toString())
                        .withDetail("reason", "Storage path is not a directory")
                        .build();
            }

            if (!Files.isWritable(storagePath)) {
                return Health.down()
                        .withDetail("path", storagePath.toString())
                        .withDetail("reason", "Storage directory is not writable")
                        .build();
            }

            // Check available disk space
            long freeSpace = storageDir.getFreeSpace();
            long totalSpace = storageDir.getTotalSpace();
            double freeSpacePercent = totalSpace > 0 ? (double) freeSpace / totalSpace * 100 : 0;

            Health.Builder builder = Health.up()
                    .withDetail("path", storagePath.toString())
                    .withDetail("freeSpace", formatBytes(freeSpace))
                    .withDetail("totalSpace", formatBytes(totalSpace))
                    .withDetail("freeSpacePercent", String.format("%.2f%%", freeSpacePercent));

            // Warn if free space is less than 10%
            if (freeSpacePercent < 10) {
                return builder
                        .status("WARN")
                        .withDetail("warning", "Low disk space")
                        .build();
            }

            return builder.build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("path", storagePath.toString())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}

