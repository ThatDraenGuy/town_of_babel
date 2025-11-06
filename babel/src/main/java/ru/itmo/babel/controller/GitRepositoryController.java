package ru.itmo.babel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.itmo.babel.dto.AnalyzeRequest;
import ru.itmo.babel.entity.GitRepositoryEntity;
import ru.itmo.babel.service.GitRepositoryService;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/repository")
public class GitRepositoryController {

    @Autowired
    private GitRepositoryService gitRepositoryService;

    @PostMapping("/analyze")
    public Map<String, Object> analyzeRepository(@RequestBody AnalyzeRequest request) throws Exception {
        GitRepositoryEntity entity = gitRepositoryService.getOrCloneRepository(request.url());

        Map<String, Object> analysis = gitRepositoryService.analyzeRepository(new File(entity.getLocalPath()));

        return Map.of(
                "repositoryId", entity.getId(),
                "repository", extractRepoName(request.url()),
                "owner", extractOwner(request.url()),
                "path", entity.getLocalPath(),
                "analysis", analysis
        );
    }

    private String extractRepoName(String url)
    {
        String[] parts = url.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "unknown";
    }

    private String extractOwner(String url)
    {
        String[] parts = url.split("/");
        return parts.length > 1 ? parts[parts.length - 2] : "unknown";
    }
}
