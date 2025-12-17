package ru.itmo.backend.exception;

/**
 * Exception thrown when Git operation fails due to access/permission issues.
 */
public class GitAccessException extends GitOperationException {
    public GitAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

