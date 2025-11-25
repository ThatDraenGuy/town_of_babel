package ru.itmo.backend.service.downloader;

import java.io.File;

public interface FileManager {

    /**
     * Recursively deletes a directory and all nested files.
     *
     * @param directory directory to delete
     */
    void deleteDirectory(File directory);

}
