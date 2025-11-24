package ru.itmo.backend.dto.response;

/**
 * DTO representing a Git branch.
 *
 * @param name         branch name
 * @param latestCommit SHA of the latest commit on the branch
 */
public record BranchDTO(String name, String latestCommit) {}
