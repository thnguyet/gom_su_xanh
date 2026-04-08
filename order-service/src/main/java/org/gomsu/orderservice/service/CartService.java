package org.gomsu.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.client.ProductClient;
import org.gomsu.orderservice.dto.ProductDTO;
import org.gomsu.orderservice.dto.response.CartResponse;
import org.gomsu.orderservice.entity.Cart;
import org.gomsu.orderservice.entity.CartItem;
import org.gomsu.orderservice.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductClient productClient;

    public CartResponse getCart(Cart cart) {
        // 1. Gom tất cả ProductId có trong giỏ hàng
        List<Long> productIds = cart.getCartItems().stream()
                .map(CartItem::getProductId) // LẤY MÃ SẢN PHẨM
                .distinct()
                .toList();

        // 2. Gọi API lấy thông tin sản phẩm hàng loạt
        List<ProductDTO> productDetails = productClient.getProductsByIds(productIds);

        // 3. Tạo Map để tra cứu nhanh
        Map<Long, ProductDTO> productMap = productDetails.stream()
                .collect(Collectors.toMap(ProductDTO::getId, p -> p));

        // 4. Chuyển đổi sang Response
        return toCartResponse(cart, productMap);
    }

    public CartResponse addToCart(Long customerId, Long productId, Integer quantity) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().customerId(customerId).build();
                    return cartRepository.save(newCart);
                });
        // Kiem tra xem san phan nay DA CO trong gio hang chua?
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(cartItem -> cartItem.getProductId().equals(productId))
                .findFirst();
        if (existingItem.isPresent()) {
            // Neu co roi -> cong don so luong moi + so luong cu
            existingItem.get().setQuantity(existingItem.get().getQuantity() + quantity);
        } else {
            // Neu chua co -> tao mot CartItem moi
            CartItem newItem = CartItem.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .cart(cart)
                    .build();
            cart.getCartItems().add(newItem);
        }
        Cart savedCart = cartRepository.save(cart);
        return getCart(savedCart);
    }

    public CartResponse updateQuantity(Long customerId, Long productId, Integer newQuantity) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Gio hang khong ton tai"));
        cart.getCartItems().stream()
                .filter(cartItem -> cartItem.getProductId().equals(productId))
                .findFirst()
                .ifPresent(cartItem -> cartItem.setQuantity(newQuantity));
        Cart savedCart = cartRepository.save(cart);
        return getCart(savedCart);
    }

    public CartResponse removeFromCart(Long customerId, Long productId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Gio hang khong ton tai"));
        cart.getCartItems().removeIf(item -> item.getProductId().equals(productId));
        Cart savedCart = cartRepository.save(cart);
        return getCart(savedCart);
    }

    public CartResponse getCartByCustomerId(Long customerId)
    {
        // Lay Cart tu DB
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Gio hang khong ton tai!"));

        // Gom tat ca productId co trong gio hang
        List<Long> productIds = cart.getCartItems().stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();

        List<ProductDTO> productDetails = productClient.getProductsByIds(productIds);

        Map<Long, ProductDTO> productMap = productDetails.stream()
                .collect(Collectors.toMap(ProductDTO::getId, p -> p));

        return toCartResponse(cart, productMap);
    }

    public CartResponse toCartResponse(Cart cart, Map<Long, ProductDTO> productMap)
    {
        List<CartResponse.CartItemResponse> itemResponses = cart.getCartItems().stream()
                .map(item -> {
                    ProductDTO productDTO = productMap.get(item.getId());
                    double price = (productDTO != null) ? productDTO.getPrice() : 0;
                    return CartResponse.CartItemResponse.builder()
                            .productId(item.getProductId())
                            .productName(productDTO != null ? productDTO.getName() : "San pham khong ton tai!")
                            .productImage(productDTO != null ? productDTO.getImageUrl() : null)
                            .unitPrice(price)
                            .quantity(item.getQuantity())
                            .subTotal(price * item.getQuantity())
                            .build();
                })
                .collect(Collectors.toList());
        return CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomerId())
                .updatedAt(cart.getUpdatedAt())
                .cartItemsResponse(itemResponses)
                .totalItems(itemResponses.size())
                .totalPrice(itemResponses.stream().mapToDouble(i -> i.getSubTotal()).sum())
                .build();
    }
}
