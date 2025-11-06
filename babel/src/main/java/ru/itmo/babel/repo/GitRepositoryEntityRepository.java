package ru.itmo.babel.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itmo.babel.entity.GitRepositoryEntity;

import java.util.Optional;

@Repository
public interface GitRepositoryEntityRepository extends JpaRepository<GitRepositoryEntity, Long> {

    Optional<GitRepositoryEntity> findByUrl(String url);
}
