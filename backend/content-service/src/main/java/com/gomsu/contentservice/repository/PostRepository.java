package com.gomsu.contentservice.repository;

import com.gomsu.contentservice.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post,Long> {
    boolean existsBySlug(String slug);
    Optional<Post> findBySlug(String slug);

    @Query("SELECT p FROM Post p WHERE " +
            "(:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:category IS NULL OR " +
            "  (CAST(p.category.id AS string) = :category OR p.category.slug = :category)) AND " +
            "(:fromDate IS NULL OR p.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR p.createdAt <= :toDate) AND " +
            "(:isAdmin = true OR p.published = true)")
    Page<Post> searchPosts(String keyword, String category,
                           LocalDateTime fromDate, LocalDateTime toDate,
                           boolean isAdmin, Pageable pageable);
}
