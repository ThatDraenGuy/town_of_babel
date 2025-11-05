package ru.itmo.babel.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@Service
public class GitRepositoryService {

    public File cloneRepository(String repoUrl) throws GitAPIException, IOException
    {
        File tempDir = Files.createTempDirectory("repo-" + UUID.randomUUID()).toFile();

        System.out.println("Clone repo to : " + repoUrl);
        Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(tempDir)
                .call();

        Repository repository = git.getRepository();
        System.out.println("Clone success: " + repository.getDirectory());

        return tempDir;
    }

    public void cleanup(File dir)
    {
        if (dir != null && dir.exists()) {
            System.out.println("Delete repo: " + dir.getAbsolutePath());
            dir.deleteOnExit();
        }
    }

    public java.util.Map<String, Object> analyzeRepository(File repoDir) throws IOException
    {
        long fileCount = Files.walk(repoDir.toPath())
                .filter(Files::isRegularFile)
                .count();

        long javaLines = Files.walk(repoDir.toPath())
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java")) // TODO заглушка для проверки.
                .mapToLong(p -> {
                    try {
                        return Files.lines(p).count();
                    } catch (IOException e) {
                        return 0;
                    }
                }).sum();

        return java.util.Map.of(
                "total_files", fileCount,
                "java_lines", javaLines
        );
    }
}