package ru.itmo.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "repositories",
       uniqueConstraints = @UniqueConstraint(columnNames = "url"))
public class GitRepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(nullable = false)
    private String localPath;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    public GitRepositoryEntity() {}

    public GitRepositoryEntity(String url, String localPath, LocalDateTime createdAt, LocalDateTime expiresAt)
    {
        this.url = url;
        this.localPath = localPath;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
