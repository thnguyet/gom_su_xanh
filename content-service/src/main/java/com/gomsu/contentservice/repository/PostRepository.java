package com.gomsu.contentservice.repository;

import com.gomsu.contentservice.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post,Long> {
    boolean existsBySlug(String slug);
    Optional<Post> findBySlug(String slug);
}
