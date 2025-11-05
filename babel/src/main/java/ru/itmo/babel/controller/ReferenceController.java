package ru.itmo.babel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.itmo.babel.dto.LanguageMetricsDTO;
import ru.itmo.babel.dto.ReferenceResponseDTO;
import ru.itmo.babel.service.ReferenceService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reference")
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
