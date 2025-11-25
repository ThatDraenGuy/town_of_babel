package ru.itmo.backend.service.downloader;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

@Service
public class JGitClient implements GitClient {
    @Override
    public void cloneProject(String url, File dir) throws GitAPIException {
        Git.cloneRepository()
                .setURI(url)
                .setDirectory(dir)
                .call();
    }

    @Override
    public void pullProject(File dir) throws GitAPIException {
        try (Git git = Git.open(dir)) {
            git.pull().call();
        } catch (Exception e) {
            throw new GitAPIException("Failed to pull repository: " + dir, e) {};
        }
    }
}
