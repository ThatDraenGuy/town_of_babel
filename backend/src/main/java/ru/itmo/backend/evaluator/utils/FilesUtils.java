package ru.itmo.backend.evaluator.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FilesUtils {
    public static Predicate<Path> haveExtension(List<String> extensions) {
        return path -> {
            String fileName = path.toString().toLowerCase();
            for (String ext : extensions) {
                if (fileName.endsWith(ext.toLowerCase())) {
                    return true;
                }
            }
            return false;
        };
    }

    public static List<Path> collectPaths(File repository, Predicate<Path> filter) throws IOException {
        try (Stream<Path> files = Files.walk(repository.toPath())) {
            return files.filter(Files::isRegularFile).filter(filter).toList();
        }
    }
}
