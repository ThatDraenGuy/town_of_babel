package ru.itmo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to analyze a GitHub repository")
public record AnalyzeRequest(
        @Schema(
                description = "URL of the GitHub repository",
                example = "https://github.com/ThatDraenGuy/ifmo_comp_math_lab1.git"
        )
        String url
) {}
