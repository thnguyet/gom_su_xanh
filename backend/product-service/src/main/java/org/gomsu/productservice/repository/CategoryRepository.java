package org.gomsu.productservice.repository;

import org.gomsu.productservice.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 1. Tìm kiếm theo tên (Dùng cho trang Admin quản lý)
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // 2. Tìm theo Slug (Dùng cho khách xem link: /categories/detail/am-chen-bat-trang)
    Optional<Category> findBySlug(String slug);

    // 3. Kiểm tra tồn tại để tránh lỗi trùng lặp khi tạo mới
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
}