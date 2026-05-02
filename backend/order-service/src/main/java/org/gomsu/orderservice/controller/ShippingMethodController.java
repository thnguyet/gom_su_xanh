package org.gomsu.orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.request.ShippingMethodRequest;
import org.gomsu.orderservice.dto.request.ShippingMethodUpdateRequest;
import org.gomsu.orderservice.dto.response.ShippingMethodResponse;
import org.gomsu.orderservice.service.ShippingMethodService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shipping-methods")
@RequiredArgsConstructor
public class ShippingMethodController {

    private final ShippingMethodService shippingMethodService;

    // Lấy danh sách đơn vị vận chuyển (THÀNH CÔNG)
    @GetMapping("/search")
    public ResponseEntity<Page<ShippingMethodResponse>> search(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(shippingMethodService.searchShippingMethods(active, keyword, from, to, page, size, sortBy, sortDir));
    }

    // Thêm đơn vị vận chuyển mới (THÀNH CÔNG)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShippingMethodResponse> create(@Valid @RequestBody ShippingMethodRequest request) {
        return ResponseEntity.ok(shippingMethodService.createShippingMethod(request));
    }

    // Cập nhật thông tin vận chuyển (THÀNH CÔNG)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShippingMethodResponse> update(@PathVariable Long id, @Valid @RequestBody ShippingMethodUpdateRequest request) {
        return ResponseEntity.ok(shippingMethodService.updateShippingMethod(id, request));
    }

    // Xóa mềm đơn vị vận chuyển (THÀNH CÔNG)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        shippingMethodService.softDeleteShippingMethod(id);
        return ResponseEntity.ok("Đã xóa mềm đơn vị vận chuyển ID: " + id);
    }

    // Cập nhật nhanh trạng thái Bật/Tắt (Switch) (THÀNH CÔNG)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShippingMethodResponse> changeStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        return ResponseEntity.ok(shippingMethodService.changeStatus(id, active));
    }
}