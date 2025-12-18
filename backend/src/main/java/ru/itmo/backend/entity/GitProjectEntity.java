package ru.itmo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects",
       uniqueConstraints = @UniqueConstraint(columnNames = "url"))
@Getter
@Setter
public class GitProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(nullable = false)
    private String localPath;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectInstanceEntity> instances = new ArrayList<>();

    public GitProjectEntity() {}

    public GitProjectEntity(String url, String localPath, LocalDateTime createdAt, LocalDateTime expiresAt)
    {
        this.url = url;
        this.localPath = localPath;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
