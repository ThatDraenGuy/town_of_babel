package ru.itmo.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.service.analysis.CodeAnalysisService;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/analysis")
@Tag(name = "Code Analysis", description = "Endpoints for analyzing project code")
public class CodeAnalysisController {

    private final CodeAnalysisService codeAnalysisService;

    public CodeAnalysisController(CodeAnalysisService codeAnalysisService) {
        this.codeAnalysisService = codeAnalysisService;
    }

    @Operation(
            summary = "Analyze a repository",
            description = "Performs static code analysis on a cloned repository",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Analysis completed successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid repository path"),
                    @ApiResponse(responseCode = "500", description = "Server error")
            }
    )
    @PostMapping
    public Map<String, Object> analyzeRepository(@RequestBody GitProjectEntity project) throws Exception {
        return codeAnalysisService.analyzeProject(new File(project.getLocalPath()));
    }
}
