package ru.itmo.backend.service.downloader;

import org.eclipse.jgit.api.errors.GitAPIException;
import ru.itmo.backend.exception.GitOperationException;

import java.io.File;

public interface GitClient {
    void cloneProject(String url, File dir) throws GitAPIException;
    void pullProject(File dir) throws GitOperationException;
}
