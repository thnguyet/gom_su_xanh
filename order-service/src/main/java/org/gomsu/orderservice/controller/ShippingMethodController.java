package org.gomsu.orderservice.controller;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.request.ShippingMethodRequest;
import org.gomsu.orderservice.dto.response.ShippingMethodResponse;
import org.gomsu.orderservice.service.ShippingMethodService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipping-methods")
@RequiredArgsConstructor
public class ShippingMethodController {

    private final ShippingMethodService shippingMethodService;

    // Lấy danh sách vận chuyển đang hoạt động
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<ShippingMethodResponse>> getAllActive() {
        return ResponseEntity.ok(shippingMethodService.getAllActiveShippingMethods());
    }

    // Thêm đơn vị vận chuyển mới
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShippingMethodResponse> create(@RequestBody ShippingMethodRequest request) {
        return ResponseEntity.ok(shippingMethodService.createShippingMethod(request));
    }

    // Cập nhật thông tin vận chuyển
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShippingMethodResponse> update(@PathVariable Long id, @RequestBody ShippingMethodRequest request) {
        return ResponseEntity.ok(shippingMethodService.updateShippingMethod(id, request));
    }

    // Xóa mềm đơn vị vận chuyển
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        shippingMethodService.softDeleteShippingMethod(id);
        return ResponseEntity.ok("Đã xóa mềm đơn vị vận chuyển ID: " + id);
    }
}