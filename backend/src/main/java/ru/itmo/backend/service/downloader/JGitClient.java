package ru.itmo.backend.service.downloader;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

@Service
public class JGitClient implements GitClient {
    @Override
    public void cloneRepo(String url, File dir) throws GitAPIException {
        Git.cloneRepository()
                .setURI(url)
                .setDirectory(dir)
                .call();
    }
}
