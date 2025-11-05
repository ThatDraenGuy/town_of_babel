package ru.itmo.babel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.itmo.babel.dto.AnalyzeRequest;
import ru.itmo.babel.service.GitRepositoryService;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/repository")
public class RepositoryController {

    @Autowired
    private GitRepositoryService gitRepositoryService;

    @PostMapping("/analyze")
    public Map<String, Object> analyzeRepository(@RequestBody AnalyzeRequest request) throws Exception {
        File repoDir = gitRepositoryService.cloneRepository(request.getUrl());
        try {
            java.util.Map<String, Object> analysis = gitRepositoryService.analyzeRepository(repoDir);

            return Map.of(
                    "repository", extractRepoName(request.getUrl()),
                    "owned", extractOwner(request.getUrl()),
                    "path", repoDir.getAbsolutePath(),
                    "analysis", analysis
            );
        } finally {
            gitRepositoryService.cleanup(repoDir);
        }
    }

    private String extractRepoName(String url) {
        String[] parts = url.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "unknown";
    }

    private String extractOwner(String url) {
        String[] parts = url.split("/");
        return parts.length > 1 ? parts[parts.length - 2] : "unknown";
    }

}
