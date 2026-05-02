package org.gomsu.productservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gomsu.productservice.dto.request.PromotionRequest;
import org.gomsu.productservice.dto.request.PromotionUpdateRequest;
import org.gomsu.productservice.dto.response.PromotionResponse;
import org.gomsu.productservice.service.PromotionService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // 1. Tạo chương trình khuyến mãi mới (THÀNH CÔNG)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponse> createPromotion(
            @RequestBody @Valid PromotionRequest request
    ) {
        return ResponseEntity.ok(promotionService.createPromotion(request));
    }

    // 2. Lấy danh sách tất cả khuyến mãi (Phân trang) (THÀNH CÔNG)
    @GetMapping
    public ResponseEntity<Page<PromotionResponse>> getAllPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        // Truyền đầy đủ tham số xuống Service
        return ResponseEntity.ok(promotionService.getAllPromotions(page, size, keyword, sortBy, sortDir, fromDate, toDate));
    }

    // Lấy chi tiết khuyến mãi theo SLUG (Dành cho trang Landing Page của khách) (THÀNH CÔNG)
    @GetMapping("/detail/{slug}")
    public ResponseEntity<PromotionResponse> getPromotionBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(promotionService.getPromotionBySlug(slug));
    }

    // 3. Xem chi tiết một chương trình khuyến mãi theo ID (THÀNH CÔNG)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<PromotionResponse> getPromotionById(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }

    // 4. Xóa chương trình khuyến mãi (THÀNH CÔNG)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok("Xóa chương trình khuyến mãi thành công!");
    }

    // 1. Endpoint Cập nhật toàn bộ thông tin khuyến mãi (THÀNH CÔNG)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable Long id,
            @RequestBody PromotionUpdateRequest request) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, request));
    }

    // 2. Endpoint Dừng khuyến mãi ngay lập tức (Chỉ thay đổi EndDate) (THÀNH CÔNG)
    @PatchMapping("/{id}/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponse> stopPromotion(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.stopPromotion(id));
    }
}