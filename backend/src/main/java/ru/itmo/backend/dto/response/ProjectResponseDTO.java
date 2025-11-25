package ru.itmo.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after cloning or retrieving a Git project")
public record ProjectResponseDTO(
        @Schema(description = "ID of the project in the database")
        Long projectId,
        @Schema(description = "GitHub repository owner")
        String owner,
        @Schema(description = "GitHub repository name")
        String project,
        @Schema(description = "Local path where the project is stored")
        String path
) {}
