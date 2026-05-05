package org.gomsu.orderservice.controller;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.response.CartResponse;
import org.gomsu.orderservice.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 1. LẤY GIỎ HÀNG (Tự hiểu User từ Token)
    // THÀNH CÔNG
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getClaim("userId").toString());
        return ResponseEntity.ok(cartService.getCartByCustomerId(userId));
    }

    // 2. THÊM SẢN PHẨM (User ID lấy từ Token)
    // THÀNH CÔNG
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        Long userId = Long.valueOf(jwt.getClaim("userId").toString());
        return ResponseEntity.ok(cartService.addToCart(userId, productId, quantity));
    }

    // 3. CẬP NHẬT SỐ LƯỢNG
    // THÀNH CÔNG
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/update")
    public ResponseEntity<CartResponse> updateQuantity(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        Long userId = Long.valueOf(jwt.getClaim("userId").toString());
        return ResponseEntity.ok(cartService.updateQuantity(userId, productId, quantity));
    }

    // 4. XÓA SẢN PHẨM
    // THÀNH CÔNG
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId) {
        Long userId = Long.valueOf(jwt.getClaim("userId").toString());
        return ResponseEntity.ok(cartService.removeFromCart(userId, productId));
    }
}