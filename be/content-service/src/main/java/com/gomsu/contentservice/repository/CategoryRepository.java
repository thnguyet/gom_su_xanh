package com.gomsu.contentservice.repository;

import com.gomsu.contentservice.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<PostCategory,Long> {
    Optional<PostCategory> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);
    Page<PostCategory> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT c FROM PostCategory c WHERE " +
            "(:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            // 1. Nếu Admin truyền activeParam thì lọc theo nó, nếu không truyền (null) thì Admin xem hết
            "(:activeParam IS NULL OR c.active = :activeParam) AND " +
            // 2. Bảo mật: Nếu không phải Admin thì BẮT BUỘC active phải là TRUE
            "(:isAdmin IS TRUE OR c.active IS TRUE)")
    Page<PostCategory> findAllByFilter(
            @Param("keyword") String keyword,
            @Param("isAdmin") Boolean isAdmin,
            @Param("activeParam") Boolean activeParam, // Thêm ở đây
            Pageable pageable);
}
