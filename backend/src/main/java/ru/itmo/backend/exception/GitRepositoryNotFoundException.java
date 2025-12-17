package ru.itmo.backend.exception;

/**
 * Exception thrown when Git repository is not found or has been deleted.
 */
public class GitRepositoryNotFoundException extends GitOperationException {
    public GitRepositoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

