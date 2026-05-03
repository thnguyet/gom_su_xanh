package com.gomsu.contentservice.repository;

import com.gomsu.contentservice.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 1. Khách xem tại trang sản phẩm: Thấy hàng CHƯA XÓA (Tạm bỏ qua duyệt để hiển thị dữ liệu cũ)
    Page<Review> findByProductIdAndIsDeletedFalse(Long productId, Pageable pageable);

    // 3. Chặn spam: Kiểm tra User đã đánh giá sản phẩm này chưa
    boolean existsByProductIdAndUserIdAndIsDeletedFalse(Long productId, Long userId);

    Optional<Review> findByProductIdAndUserIdAndIsDeletedFalse(Long productId, Long userId);

    // 4. Tính toán thống kê Rating: Chỉ tính hàng "sạch" đã duyệt
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId AND r.isDeleted = false")
    Double getAverageRatingByProductId(Long productId);

    Long countByProductIdAndIsDeletedFalse(Long productId);

    Long countByProductIdAndRatingAndIsDeletedFalse(Long productId, Integer rating);

    // --- BỔ SUNG THÊM CHO NGHIỆP VỤ MỚI ---

    // 5. User xem lại lịch sử đánh giá của chính họ (Trang cá nhân)
    Page<Review> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    // 6. Admin quản lý toàn bộ đánh giá của hệ thống (Trang Dashboard Admin)
    // Lấy tất cả đánh giá chưa bị xóa vĩnh viễn để Admin duyệt hoặc phản hồi
    Page<Review> findByIsDeletedFalse(Pageable pageable);

    // Projection để hứng dữ liệu Group By
    interface RatingCount {
        Integer getRating();
        Long getCount();
    }

    @Query("SELECT r.rating as rating, COUNT(r) as count FROM Review r " +
            "WHERE r.productId = :productId AND r.isDeleted = false " +
            "GROUP BY r.rating")
    List<RatingCount> countStarsByGroup(Long productId);
}