package org.gomsu.orderservice.controller;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.response.CartResponse;
import org.gomsu.orderservice.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 1. LẤY GIỎ HÀNG
     * GET http://localhost:8082/api/cart?customerId=1
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestParam Long customerId) {
        return ResponseEntity.ok(cartService.getCartByCustomerId(customerId));
    }

    /**
     * 2. THÊM SẢN PHẨM VÀO GIỎ (Hoặc tăng số lượng nếu đã có)
     * POST http://localhost:8082/api/cart/add?customerId=1&productId=10&quantity=2
     */
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @RequestParam Long customerId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        return ResponseEntity.ok(cartService.addToCart(customerId, productId, quantity));
    }

    /**
     * 3. CẬP NHẬT SỐ LƯỢNG (Dùng khi khách sửa trực tiếp số lượng)
     * PUT http://localhost:8082/api/cart/update?customerId=1&productId=10&quantity=5
     */
    @PutMapping("/update")
    public ResponseEntity<CartResponse> updateQuantity(
            @RequestParam Long customerId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(customerId, productId, quantity));
    }

    /**
     * 4. XÓA SẢN PHẨM KHỎI GIỎ
     * DELETE http://localhost:8082/api/cart/remove?customerId=1&productId=10
     */
    @DeleteMapping("/remove")
    public ResponseEntity<CartResponse> removeFromCart(
            @RequestParam Long customerId,
            @RequestParam Long productId) {
        return ResponseEntity.ok(cartService.removeFromCart(customerId, productId));
    }
}