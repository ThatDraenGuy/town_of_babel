package ru.itmo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_instances")
@Getter
@Setter
public class ProjectInstanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private GitProjectEntity project;

    @Column(nullable = false)
    private String localPath;

    @Column(nullable = false)
    private boolean isBusy = false;

    private LocalDateTime lastUsedAt;
}

