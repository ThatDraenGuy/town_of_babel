package ru.itmo.backend.service.downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileManagerImpl implements FileManager {

    private static final Logger log = LoggerFactory.getLogger(FileManagerImpl.class);
    private final Path storagePath;

    public FileManagerImpl(@Value("${repository.storage.path}") String storagePath) {
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Override
    public void deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }

        Path dirPath = directory.toPath().toAbsolutePath().normalize();

        // Security check: ensure directory is within storage path
        if (!dirPath.startsWith(storagePath)) {
            log.error("Attempted to delete directory outside storage path: {} (storage: {})", 
                     dirPath, storagePath);
            throw new SecurityException("Cannot delete directory outside storage path: " + dirPath);
        }

        try {
            Files.walk(dirPath)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete file/directory: {}", path, e);
                        }
                    });
            log.debug("Successfully deleted directory: {}", dirPath);
        } catch (IOException e) {
            log.error("Failed to delete directory: {}", dirPath, e);
            throw new RuntimeException("Failed to delete directory: " + dirPath, e);
        }
    }
}
