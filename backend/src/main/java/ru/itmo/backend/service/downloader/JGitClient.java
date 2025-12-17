package ru.itmo.backend.service.downloader;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.itmo.backend.exception.*;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

@Service
public class JGitClient implements GitClient {

    private static final Logger log = LoggerFactory.getLogger(JGitClient.class);

    @Override
    public void cloneProject(String url, File dir) throws GitAPIException {
        try {
            Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(dir)
                    .call();
        } catch (GitAPIException e) {
            log.error("Failed to clone repository {} to {}", url, dir, e);
            throw e;
        }
    }

    @Override
    public boolean isValidGitRepository(File dir) throws IOException {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return false;
        }
        
        try (Git git = Git.open(dir)) {
            // Try to access repository to verify it's valid
            var repo = git.getRepository();
            if (repo == null) {
                return false;
            }
            // Check if repository has at least a config file
            File gitDir = repo.getDirectory();
            return gitDir != null && gitDir.exists();
        } catch (Exception e) {
            log.debug("Directory {} is not a valid Git repository: {}", dir, e.getMessage());
            return false;
        }
    }

    @Override
    public void pullProject(File dir) throws GitOperationException {
        try (Git git = Git.open(dir)) {
            // Check if remote repository still exists
            var repo = git.getRepository();
            var config = repo.getConfig();
            var remotes = config.getSubsections("remote");
            
            // Try to fetch to check if remote exists
            try {
                git.fetch().call();
            } catch (TransportException fetchEx) {
                String fetchMessage = fetchEx.getMessage();
                if (fetchMessage != null && (
                    fetchMessage.contains("not found") ||
                    fetchMessage.contains("does not exist") ||
                    fetchMessage.contains("Repository not found")
                )) {
                    String message = "Remote repository has been deleted or is no longer accessible: " + dir;
                    log.warn(message, fetchEx);
                    throw new GitRepositoryNotFoundException(message, fetchEx);
                }
                // If fetch fails for other reasons, continue with pull
            }
            
            git.pull().call();
        } catch (RepositoryNotFoundException e) {
            String message = "Repository not found or has been deleted: " + dir;
            log.warn(message, e);
            throw new GitRepositoryNotFoundException(message, e);
        } catch (TransportException e) {
            String transportMessage = e.getMessage();
            // Check if repository was deleted on remote
            if (transportMessage != null && (
                transportMessage.contains("not found") ||
                transportMessage.contains("does not exist") ||
                transportMessage.contains("Repository not found") ||
                transportMessage.contains("remote repository is gone")
            )) {
                String errorMessage = "Remote repository has been deleted: " + dir;
                log.warn(errorMessage, e);
                throw new GitRepositoryNotFoundException(errorMessage, e);
            }
            String message = "Network error during git pull: " + dir;
            log.error(message, e);
            
            // Check for specific network issues
            Throwable cause = e.getCause();
            if (cause instanceof UnknownHostException) {
                throw new GitNetworkException("Cannot resolve host: " + cause.getMessage(), e);
            } else if (cause instanceof TimeoutException || cause instanceof java.net.SocketTimeoutException) {
                throw new GitNetworkException("Connection timeout during git pull", e);
            } else if (e.getMessage() != null && e.getMessage().contains("not authorized")) {
                throw new GitAccessException("Access denied: authentication required", e);
            } else {
                throw new GitNetworkException(message, e);
            }
        } catch (GitAPIException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.contains("conflict") || message.contains("merge")) {
                    log.warn("Merge conflict during git pull: {}", dir, e);
                    throw new GitConflictException("Merge conflict during git pull: " + dir, e);
                } else if (message.contains("not authorized") || message.contains("permission denied")) {
                    log.warn("Access denied during git pull: {}", dir, e);
                    throw new GitAccessException("Access denied: " + message, e);
                }
            }
            log.error("Git API error during pull: {}", dir, e);
            throw new GitOperationException("Git operation failed: " + message, e);
        } catch (IOException e) {
            String message = "I/O error during git pull: " + dir;
            log.error(message, e);
            throw new GitOperationException(message, e);
        } catch (Exception e) {
            String message = "Unexpected error during git pull: " + dir;
            log.error(message, e);
            throw new GitOperationException(message, e);
        }
    }
}
