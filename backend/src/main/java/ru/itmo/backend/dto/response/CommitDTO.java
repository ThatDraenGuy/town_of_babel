package ru.itmo.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a Git commit with metadata")
public record CommitDTO(

        @Schema(description = "Commit SHA hash",
                example = "a3f5b1c9d2e1f7abcd1234567890efabcd123456")
        String sha,

        @Schema(description = "Commit message", example = "Fix null pointer exception in parser")
        String message,

        @Schema(description = "Commit author name", example = "Linus Torvalds")
        String author,

        @Schema(description = "Commit timestamp in milliseconds since epoch",
                example = "1732459123000")
        long timestamp
) {}
