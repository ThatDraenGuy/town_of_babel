package ru.itmo.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to analyze a GitHub project/repository")
public record ProjectUrlDTO(
        @Schema(
                description = "URL of the GitHub project",
                example = "https://github.com/Olegshipu95/VT-Chat.git"
        )
        String url
) {}
