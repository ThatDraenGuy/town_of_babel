package ru.itmo.backend.dto.response;


/**
 * DTO representing a Git commit.
 *
 * @param sha       commit SHA
 * @param message   commit message
 * @param author    commit author
 * @param timestamp commit timestamp in milliseconds
 */
public record CommitDTO(String sha, String message, String author, long timestamp) {}