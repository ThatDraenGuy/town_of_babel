package ru.itmo.backend.dto.response;

/**
 * DTO representing a Git branch.
 *
 * @param name         branch name (short, e.g. "main" not "refs/heads/main")
 * @param latestCommit SHA of the latest commit on the branch
 */
public record BranchDTO(String name, String latestCommit) {}
