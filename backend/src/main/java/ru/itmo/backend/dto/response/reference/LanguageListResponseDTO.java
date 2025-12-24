package ru.itmo.backend.dto.response.reference;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response containing a list of languages")
public record LanguageListResponseDTO(
        @Schema(description = "List of languages")
        List<LanguageResponseDTO> items
) {}

