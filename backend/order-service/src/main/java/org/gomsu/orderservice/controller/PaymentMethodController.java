package org.gomsu.orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.request.PaymentMethodRequest;
import org.gomsu.orderservice.dto.request.PaymentMethodUpdateRequest;
import org.gomsu.orderservice.dto.response.PaymentMethodResponse;
import org.gomsu.orderservice.dto.response.ShippingMethodResponse;
import org.gomsu.orderservice.service.PaymentMethodService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    // Lấy danh sách đang hoạt động (Cho khách hàng chọn)
    // THÀNH CÔNG
    @GetMapping("/search")
    public ResponseEntity<Page<PaymentMethodResponse>> search(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(paymentMethodService.searchPaymentMethods(active, keyword, from, to, page, size, sortBy, sortDir));
    }

    // Thêm mới (Dành cho Admin)
    // THÀNH CÔNG
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentMethodResponse> create(@Valid @RequestBody PaymentMethodRequest request) {
        return ResponseEntity.ok(paymentMethodService.createPaymentMethod(request));
    }

    // Cập nhật tên
    // THÀNH CÔNG
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentMethodResponse> update(@PathVariable Long id, @Valid @RequestBody PaymentMethodUpdateRequest request) {
        return ResponseEntity.ok(paymentMethodService.updatePaymentMethod(id, request));
    }

    // Xóa mềm
    // THÀNH CÔNG
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        paymentMethodService.softDeletePaymentMethod(id);
        return ResponseEntity.ok("Đã xóa mềm phương thức thanh toán ID: " + id);
    }

    // Cập nhật nhanh trạng thái Bật/Tắt (Switch) (THÀNH CÔNG)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentMethodResponse> changeStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        return ResponseEntity.ok(paymentMethodService.changeStatus(id, active));
    }
}