package ru.itmo.babel.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/repository")
public class CommitController {

    @GetMapping("/commits")
    public Map<String, Object> getCommits(
            @RequestParam String url,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<String> metrics
    ) {
        // TODO: вызывать сервис анализа коммитов (https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/WalkAllCommits.java)
        return Map.of(
                "repository", "project-name",
                "page", page,
                "size", size,
                "total_commits", 354,
                "commits", List.of(
                        Map.of(
                                "hash", "a8f3b1c",
                                "author", "Alice",
                                "date", "2025-10-10T12:15:33Z",
                                "message", "Refactor analyzer core",
                                "metrics", Map.of(
                                        "complexity", 2.5,
                                        "coverage", 0.86
                                )
                        ),
                        Map.of(
                                "hash", "b4e32d9",
                                "author", "Bob",
                                "date", "2025-10-09T09:47:21Z",
                                "message", "Add logging",
                                "metrics", Map.of(
                                        "complexity", 1.3,
                                        "coverage", 0.79
                                )
                        )
                )
        );
    }

    @GetMapping("/commit/{hash}")
    public Map<String, Object> getCommit(
            @PathVariable String hash,
            @RequestParam String url,
            @RequestParam(required = false) List<String> metrics
    ) {
        return Map.of(
                "repository", "project-name",
                "commit", hash,
                "author", "Alice",
                "date", "2025-10-10T12:15:33Z",
                "metrics", Map.of(
                        "complexity", 2.5,
                        "loc", 540,
                        "comments", 0.18
                )
        );
    }
}
