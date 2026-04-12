package org.gomsu.orderservice.controller;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.request.OrderRequest;
import org.gomsu.orderservice.dto.response.OrderResponse;
import org.gomsu.orderservice.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
