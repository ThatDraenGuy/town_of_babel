package ru.itmo.backend.dto.response.reference;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a programming language supported by the system")
public record LanguageResponseDTO(
        @Schema(description = "Unique language identifier")
        Long languageId,
        @Schema(description = "Language code", example = "java")
        String languageCode,
        @Schema(description = "Human-readable language name", example = "Java")
        String languageName
) {}

