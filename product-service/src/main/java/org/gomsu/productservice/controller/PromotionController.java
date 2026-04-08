package org.gomsu.productservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gomsu.productservice.dto.request.PromotionRequest;
import org.gomsu.productservice.dto.response.PromotionResponse;
import org.gomsu.productservice.service.PromotionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // 1. Tạo chương trình khuyến mãi mới
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponse> createPromotion(
            @RequestBody @Valid PromotionRequest request
    ) {
        return ResponseEntity.ok(promotionService.createPromotion(request));
    }

    // 2. Lấy danh sách tất cả khuyến mãi (Phân trang)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<PromotionResponse>> getAllPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(promotionService.getAllPromotions(page, size));
    }

    // 3. Xem chi tiết một chương trình khuyến mãi theo ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<PromotionResponse> getPromotionById(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }

    // 4. Xóa chương trình khuyến mãi
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok("Xóa chương trình khuyến mãi thành công!");
    }

    // 1. Endpoint Cập nhật toàn bộ thông tin khuyến mãi
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable Long id,
            @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, request));
    }

    // 2. Endpoint Dừng khuyến mãi ngay lập tức (Chỉ thay đổi EndDate)
    @PatchMapping("/{id}/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponse> stopPromotion(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.stopPromotion(id));
    }
}