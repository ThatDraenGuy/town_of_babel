package ru.itmo.backend.service.downloader;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class FileManagerImpl implements FileManager {

    @Override
    public void deleteDirectory(File directory) {
        if (directory == null || !directory.exists())
            return;

        try {
            Files.walk(directory.toPath())
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }
}
