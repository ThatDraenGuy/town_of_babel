package ru.itmo.backend.dto.response.commit;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a Git branch with its latest commit")
public record BranchDTO(

        @Schema(description = "Short branch name", example = "main")
        String name,

        @Schema(description = "SHA hash of the latest commit on the branch",
                example = "4f1d2ab99cd93b781e22c14fa3d45af33bbcd123")
        String latestCommit
) {}
