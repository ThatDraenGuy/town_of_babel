package ru.itmo.backend.service.downloader;

import org.eclipse.jgit.api.errors.GitAPIException;
import ru.itmo.backend.exception.GitOperationException;

import java.io.File;
import java.io.IOException;

public interface GitClient {
    void cloneProject(String url, File dir) throws GitAPIException;
    void cloneLocal(File source, File destination) throws GitAPIException;
    void pullProject(File dir) throws GitOperationException;
    void checkout(File dir, String commitSha) throws GitOperationException;
    
    /**
     * Validates that the given directory is a valid Git repository.
     *
     * @param dir directory to validate
     * @return true if directory is a valid Git repository
     * @throws IOException if I/O error occurs
     */
    boolean isValidGitRepository(File dir) throws IOException;
}
