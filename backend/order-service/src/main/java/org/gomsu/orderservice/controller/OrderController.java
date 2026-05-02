package org.gomsu.orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.request.OrderRequest;
import org.gomsu.orderservice.dto.response.OrderResponse;
import org.gomsu.orderservice.entity.OrderStatus;
import org.gomsu.orderservice.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    // THÀNH CÔNG
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OrderRequest orderRequest
    ){
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(orderService.createOrder(userId, orderRequest));
    }

    // THÀNH CÔNG
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Cả User và Admin đều có quyền hủy
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Jwt jwt) {

        // Lấy userId từ Token để đảm bảo tính bảo mật (User chỉ được hủy đơn của chính mình)
        Long userId = jwt.getClaim("userId");

        // Kiểm tra xem trong Token có Role ADMIN không
        String scope = jwt.getClaim("scope");
        boolean isAdmin = scope != null && scope.equals("ADMIN");

        // Gọi xuống Service để xử lý logic hủy và bắn tin nhắn RabbitMQ
        OrderResponse response = orderService.cancelOrder(userId, orderId, isAdmin);

        return ResponseEntity.ok(response);
    }

    // THÀNH CÔNG
    @GetMapping("/my-orders")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        // Lấy ID từ token (khớp với key "userId" trong Identity Service của Nguyệt)
        Long customerId = Long.valueOf(jwt.getClaim("userId").toString());

        // Gọi hàm getAllOrdersForAdmin nhưng truyền customerId của chính mình vào
        // Như vậy người dùng sẽ lọc được cả ngày tháng luôn!
        Page<OrderResponse> orders = orderService.getAllOrdersForAdmin(
                customerId, status, startDate, endDate, page, size, sortBy, sortDir);

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/all-orders")
    public ResponseEntity<Page<OrderResponse>> getAllOrdersForAdmin(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Page<OrderResponse> orders = orderService.getAllOrdersForAdmin(
                customerId,
                status,
                startDate,
                endDate,
                page,
                size,
                sortBy,
                sortDir
        );

        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @AuthenticationPrincipal Jwt jwt
    ) {
        // Lấy ID của Admin từ Token (để lưu log hoặc xử lý logic nếu cần)
        Long adminId = Long.valueOf(jwt.getClaim("userId").toString());

        OrderResponse response = orderService.updateOrderStatus(orderId, status, adminId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }
}
