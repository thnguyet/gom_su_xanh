package org.gomsu.orderservice.controller;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.request.PaymentMethodRequest;
import org.gomsu.orderservice.dto.response.PaymentMethodResponse;
import org.gomsu.orderservice.service.PaymentMethodService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    // Lấy danh sách đang hoạt động (Cho khách hàng chọn)
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PaymentMethodResponse>> getAllActive() {
        return ResponseEntity.ok(paymentMethodService.getAllActivePaymentMethods());
    }

    // Thêm mới (Dành cho Admin)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentMethodResponse> create(@RequestBody PaymentMethodRequest request) {
        return ResponseEntity.ok(paymentMethodService.createPaymentMethod(request));
    }

    // Cập nhật tên
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentMethodResponse> update(@PathVariable Long id, @RequestBody PaymentMethodRequest request) {
        return ResponseEntity.ok(paymentMethodService.updatePaymentMethod(id, request));
    }

    // Xóa mềm
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        paymentMethodService.softDeletePaymentMethod(id);
        return ResponseEntity.ok("Đã xóa mềm phương thức thanh toán ID: " + id);
    }
}