package com.gomsu.contentservice.service;

import com.gomsu.contentservice.client.OrderClient;
import com.gomsu.contentservice.client.ProductClient;
import com.gomsu.contentservice.configuration.RabbitMQConfig;
import com.gomsu.contentservice.dto.OrderDTO;
import com.gomsu.contentservice.dto.ProductDTO;
import com.gomsu.contentservice.dto.ReviewUpdateEvent;
import com.gomsu.contentservice.dto.request.ReviewReplyRequest;
import com.gomsu.contentservice.dto.request.ReviewRequest;
import com.gomsu.contentservice.dto.request.ReviewUpdateRequest;
import com.gomsu.contentservice.dto.response.ReviewResponse;
import com.gomsu.contentservice.entity.Review;
import com.gomsu.contentservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ProductClient productClient;
    private final OrderClient orderClient;

    // --- 1. LẤY ĐÁNH GIÁ THEO SẢN PHẨM (Sửa để hết lỗi đỏ ở Controller) ---
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByProduct(Long productId, boolean isAdmin, Pageable pageable) {
        // Lấy tên SP 1 lần duy nhất để tránh lỗi N+1 (gọi API liên tục trong vòng lặp)
        String productName = getProductName(productId);

        Page<Review> reviews;
        if (isAdmin) {
            // Admin thấy cả đánh giá chờ duyệt nhưng chưa xóa
            reviews = reviewRepository.findByProductIdAndIsDeletedFalse(productId, pageable);
        } else {
            // Khách chỉ thấy đánh giá đã duyệt và chưa xóa
            reviews = reviewRepository.findByProductIdAndIsApprovedTrueAndIsDeletedFalse(productId, pageable);
        }

        return reviews.map(review -> toResponse(review, productName));
    }

    // --- 2. USER TẠO ĐÁNH GIÁ ---
    @Transactional
    public ReviewResponse createReview(ReviewRequest request, Long userId, String username) {
        // 1. Kiểm tra đơn hàng thông qua FeignClient
        if (request.getOrderId() != null) {
            try {
                OrderDTO order = orderClient.getOrderById(request.getOrderId());

                // Chỉ cho phép review nếu đơn hàng đã hoàn tất (COMPLETED)
                if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
                    throw new RuntimeException("Đơn hàng chưa hoàn tất. Bạn cần nhận hàng trước khi đánh giá!");
                }

                // Bảo mật: Đơn hàng phải thuộc về người đang đăng nhập
                if (!order.getCustomerId().equals(userId)) {
                    throw new RuntimeException("Đơn hàng này không thuộc về bạn!");
                }
            } catch (Exception e) {
                log.error("Lỗi xác thực đơn hàng: {}", e.getMessage());
                throw new RuntimeException("Không thể xác minh thông tin mua hàng.");
            }
        }

        // 2. Chặn trùng (Mỗi user chỉ review 1 sản phẩm 1 lần)
        if (reviewRepository.existsByProductIdAndUserIdAndIsDeletedFalse(request.getProductId(), userId)) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi!");
        }

        // 3. Map dữ liệu vào Entity (Giữ String imageReview)
        Review review = Review.builder()
                .productId(request.getProductId())
                .userId(userId)
                .username(username)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .imageReview(request.getImageReview()) // Vẫn dùng String
                .orderId(request.getOrderId())
                .isApproved(true) // Có Order ID hợp lệ thì mặc định duyệt
                .build();

        Review saved = reviewRepository.save(review);

        // 4. Đồng bộ số sao sang Product Service qua RabbitMQ
        syncProductRating(saved.getProductId());

        return toResponse(saved, getProductName(saved.getProductId()));
    }

    // --- 3. USER SỬA ĐÁNH GIÁ ---
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại"));

        // 1. Kiểm tra chính chủ
        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa!");
        }

        // 2. Chỉ cập nhật những gì User gửi lên (Check null)
        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null && !request.getComment().isBlank()) {
            review.setComment(request.getComment());
        }
        if (request.getTitle() != null) {
            review.setTitle(request.getTitle());
        }
        if (request.getImageReview() != null) {
            review.setImageReview(request.getImageReview());
        }

        Review updated = reviewRepository.save(review);

        // 3. Đồng bộ lại sang Product Service
        syncProductRating(updated.getProductId());

        return toResponse(updated, getProductName(updated.getProductId()));
    }

    // --- 4. LẤY LỊCH SỬ CỦA TÔI ---
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(Long userId, Pageable pageable) {
        // Ở đây mình lấy productId từ review để lấy tên SP (chấp nhận gọi fetch tên SP)
        return reviewRepository.findByUserIdAndIsDeletedFalse(userId, pageable)
                .map(review -> toResponse(review, getProductName(review.getProductId())));
    }

    // --- 5. ADMIN LẤY TẤT CẢ ---
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviewsForAdmin(Pageable pageable) {
        return reviewRepository.findByIsDeletedFalse(pageable)
                .map(review -> toResponse(review, getProductName(review.getProductId())));
    }

    // --- 6. ADMIN PHẢN HỒI ---
    @Transactional
    public ReviewResponse replyReview(Long reviewId, ReviewReplyRequest replyRequest) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        review.setAdminReply(replyRequest.getAdminReply());
        review.setRepliedAt(LocalDateTime.now());

        return toResponse(reviewRepository.save(review), getProductName(review.getProductId()));
    }

    // --- 7. XÓA MỀM ---
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại"));

        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này!");
        }

        review.setDeleted(true);
        reviewRepository.save(review);
        syncProductRating(review.getProductId());
    }

    // --- PRIVATE HELPERS ---

    private void syncProductRating(Long productId) {
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        Long count = reviewRepository.countByProductIdAndIsApprovedTrueAndIsDeletedFalse(productId);

        ReviewUpdateEvent event = new ReviewUpdateEvent(productId, avg != null ? avg : 0.0, count);
        rabbitTemplate.convertAndSend(RabbitMQConfig.REVIEW_EXCHANGE, RabbitMQConfig.REVIEW_ROUTING_KEY, event);
    }

    private String getProductName(Long productId) {
        try {
            ProductDTO dto = productClient.getProductById(productId);
            return (dto != null) ? dto.getName() : "Sản phẩm Gốm Sứ";
        } catch (Exception e) {
            log.warn("Không lấy được tên SP cho ID {}: {}", productId, e.getMessage());
            return "Sản phẩm Gốm Sứ";
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReviewStats(Long productId) {
        // 1. Lấy tất cả thống kê sao trong 1 lần gọi DB
        List<ReviewRepository.RatingCount> ratingCounts = reviewRepository.countStarsByGroup(productId);

        // 2. Khởi tạo map với giá trị mặc định là 0 cho 5 mức sao
        Map<Integer, Long> starStats = new HashMap<>();
        for (int i = 1; i <= 5; i++) starStats.put(i, 0L);

        // 3. Đổ dữ liệu từ DB vào map
        long totalReviews = 0;
        for (ReviewRepository.RatingCount rc : ratingCounts) {
            starStats.put(rc.getRating(), rc.getCount());
            totalReviews += rc.getCount();
        }

        // 4. Lấy điểm trung bình (có thể dùng lại hàm cũ hoặc tính trực tiếp ở đây)
        Double avg = reviewRepository.getAverageRatingByProductId(productId);

        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("averageRating", avg != null ? avg : 0.0);
        response.put("totalReviews", totalReviews);
        response.put("stars", starStats);

        return response;
    }

    private ReviewResponse toResponse(Review review, String productName) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .productName(productName)
                .username(review.getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageReview(review.getImageReview())
                .isVerifiedPurchase(review.getOrderId() != null)
                .adminReply(review.getAdminReply())
                .repliedAt(review.getRepliedAt())
                .createdAt(review.getCreatedAt())
                .isApproved(review.isApproved())
                .build();
    }
}