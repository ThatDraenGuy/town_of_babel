package ru.itmo.backend.exception;

/**
 * Base exception for Git operations.
 */
public class GitOperationException extends Exception {
    public GitOperationException(String message) {
        super(message);
    }

    public GitOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

