package org.gomsu.orderservice.controller;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.request.OrderRequest;
import org.gomsu.orderservice.dto.response.OrderResponse;
import org.gomsu.orderservice.entity.OrderStatus;
import org.gomsu.orderservice.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody OrderRequest orderRequest
    ){
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(orderService.createOrder(userId, orderRequest));
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Cả User và Admin đều có quyền hủy
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Jwt jwt) {

        // Lấy userId từ Token để đảm bảo tính bảo mật (User chỉ được hủy đơn của chính mình)
        Long userId = jwt.getClaim("userId");

        // Gọi xuống Service để xử lý logic hủy và bắn tin nhắn RabbitMQ
        OrderResponse response = orderService.cancelOrder(userId, orderId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Long customerId = jwt.getClaim("userId");

        Page<OrderResponse> orders = orderService.getMyOrders(customerId, status, page, size, sortBy, sortDir);

        return ResponseEntity.ok(orders);
    }
}
