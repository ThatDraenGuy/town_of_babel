package ru.itmo.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itmo.backend.entity.GitProjectEntity;
import ru.itmo.backend.service.analysis.CodeAnalysisService;
import ru.itmo.backend.service.downloader.ProjectAccessService;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/analysis")
@Tag(name = "Code Analysis", description = "Endpoints for analyzing project code")
public class CodeAnalysisController {

    private final CodeAnalysisService codeAnalysisService;
    private final ProjectAccessService projectAccessService;

    public CodeAnalysisController(CodeAnalysisService codeAnalysisService, ProjectAccessService projectAccessService) {
        this.codeAnalysisService = codeAnalysisService;
        this.projectAccessService = projectAccessService;
    }

    @Operation(summary = "Analyze a project", description = "Performs static code analysis on the entire cloned project (last commit on main)")
    @PostMapping("/project/{projectId}")
    public String analyzeProject(@PathVariable Long projectId, @RequestParam List<String> languages) throws Exception {
        return codeAnalysisService.analyzeProject(projectId, languages);
    }

    @Operation(summary = "Get most popular language", description = "Fetches most popular language from Github API")
    @GetMapping("/project/{projectId}/language")
    public String analyzeLanguage(@PathVariable Long projectId, @RequestParam Double threshold) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode response = mapper.createObjectNode();

        try {
            Optional<String> result = codeAnalysisService.getMostPopularLanguageFromGitHub(projectAccessService.getById(projectId).getUrl());

            if (result.isPresent()) {
                response.put("status", "ok");
                response.put("language", result.get());
            } else {
                response.put("status", "unknown");
            }
        } catch (IOException | InterruptedException e) {
            response.put("status", "failure");
        }

        return mapper.writeValueAsString(response);
    }

    @Operation(summary = "Analyze a specific commit", description = "Performs static code analysis on the code at a given commit")
    @PostMapping("/project/{projectId}/commit/{commitSha}")
    public Map<String, Object> analyzeCommit(@PathVariable Long projectId, @PathVariable String commitSha) throws Exception {
        return codeAnalysisService.analyzeCommit(projectId, commitSha);
    }

    @Operation(summary = "Analyze diff between commits", description = "Performs analysis on the diff between two commits")
    @PostMapping("/project/{projectId}/diff")
    public Map<String, Object> analyzeDiff(@PathVariable Long projectId, @RequestParam String baseCommit, @RequestParam String targetCommit) throws Exception {
        return codeAnalysisService.analyzeDiff(projectId, baseCommit, targetCommit);
    }
}
