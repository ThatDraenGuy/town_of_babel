package ru.itmo.backend.dto.response.commit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response containing a list of branches")
public record BranchResponseDTO(
        @Schema(description = "List of branches")
        List<BranchDTO> items
) {}

