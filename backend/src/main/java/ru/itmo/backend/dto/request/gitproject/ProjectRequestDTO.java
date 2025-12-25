package ru.itmo.backend.dto.request.gitproject;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to analyze a GitHub project/repository")
public record ProjectRequestDTO(
        @Schema(
                description = "URL of the GitHub project",
                example = "https://github.com/Olegshipu95/VT-Chat.git"
        )
        @NotBlank(message = "URL cannot be blank")
        @Pattern(regexp = "^(https?|git)://.*\\.git$|^https?://.*/([^/]+)/([^/]+)(\\.git)?/?$", 
                 message = "Invalid Git repository URL format")
        String url
) {}
