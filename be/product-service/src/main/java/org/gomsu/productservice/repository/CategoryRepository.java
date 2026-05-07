package org.gomsu.productservice.repository;

import org.gomsu.productservice.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 1. Tìm kiếm theo tên hoặc ID (Dùng cho trang Admin quản lý)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Category c WHERE " +
            "CAST(c.id AS string) LIKE %:keyword% OR " +
            "LOWER(c.name) LIKE LOWER(concat('%', :keyword, '%'))")
    Page<Category> findByIdOrName(String keyword, Pageable pageable);

    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // 2. Tìm theo Slug (Dùng cho khách xem link: /categories/detail/am-chen-bat-trang)
    Optional<Category> findBySlug(String slug);

    // 3. Kiểm tra tồn tại để tránh lỗi trùng lặp khi tạo mới
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
    // 4. Tìm các danh mục đang hoạt động
    java.util.List<Category> findByActiveTrue();
}