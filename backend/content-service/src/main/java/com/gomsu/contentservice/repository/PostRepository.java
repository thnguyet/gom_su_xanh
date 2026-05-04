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
            "(:keyword IS NULL OR :keyword = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR CONCAT(p.id, '') LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:category IS NULL OR " +
            "  (CONCAT(p.category.id, '') = :category OR p.category.slug = :category)) AND " +
            "(:fromDate IS NULL OR p.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR p.createdAt <= :toDate) AND " +
            "(:published IS NULL OR p.published = :published) AND " +
            "(:isAdmin = true OR p.published = true)")
    Page<Post> searchPosts(String keyword, String category,
                           LocalDateTime fromDate, LocalDateTime toDate,
                           Boolean published, boolean isAdmin, Pageable pageable);
}
