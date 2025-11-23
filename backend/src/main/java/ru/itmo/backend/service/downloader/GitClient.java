package ru.itmo.backend.service.downloader;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;

public interface GitClient {
    void cloneRepo(String url, File dir) throws GitAPIException;
}
