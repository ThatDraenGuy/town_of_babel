package ru.itmo.backend.controller;

import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.dto.response.reference.LanguageMetricsDTO;
import ru.itmo.backend.service.reference.ReferenceService;

import java.util.List;

@RestController
@RequestMapping("/reference")
public class ReferenceController {

    private final ReferenceService referenceService;

    public ReferenceController(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @GetMapping
    public List<LanguageMetricsDTO> getReference() {
        return referenceService.getReference();
    }
}
