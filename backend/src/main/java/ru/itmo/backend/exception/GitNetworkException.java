package ru.itmo.backend.exception;

/**
 * Exception thrown when Git operation fails due to network issues.
 */
public class GitNetworkException extends GitOperationException {
    public GitNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}

