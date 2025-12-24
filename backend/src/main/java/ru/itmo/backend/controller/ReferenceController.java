package ru.itmo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.response.reference.LanguageListResponseDTO;
import ru.itmo.backend.dto.response.reference.LanguageMetricsResponseDTO;
import ru.itmo.backend.service.reference.ReferenceService;

@RestController
@RequestMapping
public class ReferenceController {

    private final ReferenceService referenceService;

    public ReferenceController(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @Operation(operationId = "getLanguages")
    @GetMapping("/languages")
    public LanguageListResponseDTO getLanguages() {
        return new LanguageListResponseDTO(referenceService.getLanguages());
    }

    @Operation(operationId = "getMetrics")
    @GetMapping("/languages/{languageCode}/metrics")
    public ResponseEntity<LanguageMetricsResponseDTO> getMetrics(@PathVariable String languageCode) {
        return referenceService.getLanguageMetrics(languageCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
