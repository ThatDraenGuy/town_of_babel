package ru.itmo.backend.config;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.backend.exception.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("IllegalArgumentException: {}", ex.getMessage());
        
        // Determine appropriate status code based on context
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex.getMessage() != null && ex.getMessage().contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        }

        return ResponseEntity.status(status)
                .body(Map.of(
                        "error", status == HttpStatus.NOT_FOUND ? "Not Found" : "Bad Request",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Invalid argument"
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Validation Failed",
                        "message", "Invalid input parameters",
                        "details", errors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        logger.warn("Constraint violation: {}", ex.getMessage());
        
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage()
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Validation Failed",
                        "message", "Invalid input parameters",
                        "details", errors
                ));
    }

    @ExceptionHandler(GitRepositoryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleGitRepositoryNotFound(GitRepositoryNotFoundException ex) {
        logger.warn("Git repository not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "Repository Not Found",
                        "message", "The requested Git repository was not found or has been deleted"
                ));
    }

    @ExceptionHandler(GitNetworkException.class)
    public ResponseEntity<Map<String, Object>> handleGitNetworkError(GitNetworkException ex) {
        logger.error("Git network error: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Network Error",
                        "message", "Failed to connect to Git repository. Please try again later."
                ));
    }

    @ExceptionHandler(GitAccessException.class)
    public ResponseEntity<Map<String, Object>> handleGitAccessError(GitAccessException ex) {
        logger.warn("Git access error: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "Access Denied",
                        "message", "Access to the Git repository is denied. Authentication may be required."
                ));
    }

    @ExceptionHandler(GitConflictException.class)
    public ResponseEntity<Map<String, Object>> handleGitConflict(GitConflictException ex) {
        logger.warn("Git conflict: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "Merge Conflict",
                        "message", "Unable to update repository due to merge conflicts"
                ));
    }

    @ExceptionHandler(GitOperationException.class)
    public ResponseEntity<Map<String, Object>> handleGitOperationError(GitOperationException ex) {
        logger.error("Git operation error: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Git Operation Failed",
                        "message", "An error occurred during Git operation"
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        logger.warn("ResponseStatusException ({}): {}", ex.getStatusCode(), ex.getReason());

        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of(
                        "error", ex.getReason() != null ? ex.getReason() : "Error",
                        "message", ex.getMessage() != null ? ex.getMessage() : "An error occurred"
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralError(Exception ex) {
        logger.error("Unexpected exception occurred", ex);

        // Don't expose internal error details to client
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal Server Error",
                        "message", "An unexpected error occurred. Please try again later."
                ));
    }
}
