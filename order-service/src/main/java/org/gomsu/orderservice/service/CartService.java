package org.gomsu.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.client.ProductClient;
import org.gomsu.orderservice.dto.ProductDTO;
import org.gomsu.orderservice.dto.response.CartResponse;
import org.gomsu.orderservice.entity.Cart;
import org.gomsu.orderservice.entity.CartItem;
import org.gomsu.orderservice.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductClient productClient;

    // Hàm lấy chi tiết giỏ hàng và gọi sang Product Service
    public CartResponse getCartByCustomerId(Long customerId) {
        // Tìm giỏ hàng, nếu chưa có thì tạo mới luôn cho khách
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(Cart.builder().customerId(customerId).build()));

        List<CartItem> cartItems = cart.getCartItems() != null ? cart.getCartItems() : new ArrayList<>();

        // 1. Thu thập danh sách ID sản phẩm có trong giỏ
        List<Long> productIds = cart.getCartItems().stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();

        // 2. Gọi sang Product Service lấy thông tin (Giá, Tên, Ảnh...)
        Map<Long, ProductDTO> productMap = Map.of();
        if (!productIds.isEmpty()) {
            List<ProductDTO> productDetails = productClient.getProductsByIds(productIds);
            productMap = productDetails.stream()
                    .collect(Collectors.toMap(ProductDTO::getId, p -> p));
        }

        // 3. Chuyển đổi sang Response trả về cho Frontend
        return toCartResponse(cart, productMap);
    }

    // Them san pham vao gio hang
    public CartResponse addToCart(Long customerId, Long productId, Integer quantity) {
        // Tim gio hang theo customerId
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(Cart.builder().customerId(customerId).build()));

        // Tim trong gio hang co nhung san pham nao?
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        // Neu gio hang co san pham do roi thi tang so luong len
        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + quantity);
        } else {
            // Neu chua co san pham do thi tao san pham moi vao
            CartItem newItem = CartItem.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .cart(cart)
                    .build();
            cart.getCartItems().add(newItem);
        }

        cartRepository.save(cart);
        return getCartByCustomerId(customerId);
    }

    // Cap nhat so luong cho san pham co trong gio hang
    public CartResponse updateQuantity(Long customerId, Long productId, Integer newQuantity) {
        // Tim gio hang cua customerId
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));

        // Lay san pham trong gio hang va neu ton tai thi tang so luong len theo newQuantity
        cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(newQuantity));

        cartRepository.save(cart);
        return getCartByCustomerId(customerId);
    }

    // Xoa san pham ra khoi gio hang
    public CartResponse removeFromCart(Long customerId, Long productId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));

        cart.getCartItems().removeIf(item -> item.getProductId().equals(productId));

        cartRepository.save(cart);
        return getCartByCustomerId(customerId);
    }

    private CartResponse toCartResponse(Cart cart, Map<Long, ProductDTO> productMap) {
        List<CartResponse.CartItemResponse> itemResponses = cart.getCartItems().stream()
                .map(item -> {
                    ProductDTO dto = productMap.get(item.getProductId()); // Đã sửa: lấy theo ProductId
                    double price = (dto != null) ? dto.getPrice() : 0;

                    return CartResponse.CartItemResponse.builder()
                            .productId(item.getProductId())
                            .productName(dto != null ? dto.getName() : "Sản phẩm không tồn tại")
                            .productImage(dto != null ? dto.getImageUrl() : null)
                            .unitPrice(price)
                            .quantity(item.getQuantity())
                            .subTotal(price * item.getQuantity())
                            .build();
                })
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomerId())
                .cartItemsResponse(itemResponses)
                .totalPrice(itemResponses.stream().mapToDouble(CartResponse.CartItemResponse::getSubTotal).sum())
                .totalItems(itemResponses.size())
                .build();
    }
}
