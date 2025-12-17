package ru.itmo.backend.exception;

/**
 * Exception thrown when Git operation fails due to merge conflicts.
 */
public class GitConflictException extends GitOperationException {
    public GitConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

