package ru.itmo.babel.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reference")
public class ReferenceController {

    @GetMapping
    public Map<String, Object> getReference() {
        // TODO: в будущем из config файла
        return Map.of(
                "languages", List.of("Java", "Kotlin", "Python", "JavaScript", "C++"),
                "metrics", List.of(
                        Map.of("id", "loc", "name", "Lines of Code", "description", "Количество строк кода"),
                        Map.of("id", "complexity", "name", "Cyclomatic Complexity", "description", "Средняя цикломатическая сложность"),
                        Map.of("id", "comments", "name", "Comment Density", "description", "Отношение комментариев к строкам кода")
                )
        );
    }
}
