package org.gomsu.productservice.repository;

import org.gomsu.productservice.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion,Long> {
    @Query("SELECT p FROM Promotion p WHERE " +
            "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR p.startDate >= :fromDate) AND " +
            "(:toDate IS NULL OR p.endDate <= :toDate)")
    Page<Promotion> searchPromotions(
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    // Đừng quên hàm này để lấy chi tiết theo Slug cho khách hàng
    Optional<Promotion> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
