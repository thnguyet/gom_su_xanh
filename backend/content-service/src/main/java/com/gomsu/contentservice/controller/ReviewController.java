package com.gomsu.contentservice.controller;

import com.gomsu.contentservice.dto.request.ReviewReplyRequest;
import com.gomsu.contentservice.dto.request.ReviewRequest;
import com.gomsu.contentservice.dto.request.ReviewUpdateRequest;
import com.gomsu.contentservice.dto.response.ReviewResponse;
import com.gomsu.contentservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {
    private final ReviewService reviewService;

    // 1. USER: Tạo mới
    // THÀNH CÔNG
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewResponse> create(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(request, extractUserId(jwt), jwt.getClaimAsString("sub")));
    }

    // 2. USER: Sửa (Chính chủ)
    // THÀNH CÔNG
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(reviewService.updateReview(id, request, extractUserId(jwt)));
    }

    // 3. USER: Xóa mềm
    // THÀNH CÔNG
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        reviewService.deleteReview(id, extractUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    // 4. USER: Xem lịch sử đánh giá cá nhân
    // THÀNH CÔNG
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getMyReviews(extractUserId(jwt), pageable));
    }

    // 5. ADMIN: Quản lý toàn bộ hệ thống
    // THÀNH CÔNG
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReviewResponse>> getAllForAdmin(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getAllReviewsForAdmin(pageable));
    }

    // 6. ADMIN: Phản hồi khách hàng
    @PutMapping("/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> reply(
            @PathVariable Long id,
            @Valid @RequestBody ReviewReplyRequest replyRequest) {
        return ResponseEntity.ok(reviewService.replyReview(id, replyRequest));
    }

    // 7. ADMIN: Xóa đánh giá
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteByAdmin(@PathVariable Long id) {
        reviewService.deleteReviewByAdmin(id);
        return ResponseEntity.noContent().build();
    }

    // 7. PUBLIC/ADMIN: Xem đánh giá của sản phẩm (ĐÃ SỬA LOGIC CHECK ADMIN)
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            @AuthenticationPrincipal Jwt jwt, // Thêm vào để check quyền
            @PageableDefault(size = 10) Pageable pageable) {

        boolean isAdmin = false;
        if (jwt != null) {
            String scope = jwt.getClaimAsString("scope");
            isAdmin = (scope != null && scope.contains("ADMIN"));
        }

        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, isAdmin, pageable));
    }

    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<Map<String, Object>> getProductReviewStats(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewStats(productId));
    }

    @GetMapping("/my-review/{productId}")
    public ResponseEntity<ReviewResponse> getMyReview(@PathVariable Long productId,
                                                       @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();
        Long userId = extractUserId(jwt);
        ReviewResponse review = reviewService.getUserReviewForProduct(productId, userId);
        return ResponseEntity.ok(review);
    }

    private Long extractUserId(Jwt jwt) {
        Object userId = jwt.getClaim("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return Long.valueOf(userId.toString());
    }
}