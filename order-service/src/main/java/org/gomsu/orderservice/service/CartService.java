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
        // Trong hàm getCartByCustomerId và addToCart, sửa lại đoạn orElseGet:
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomerId(customerId);
                    newCart.setCartItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });

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

        // 1. Gọi Product Service để check kho trước (Dùng Feign Client)
        // Giả sử Nguyệt đã có hàm getProductById bên ProductClient
        ProductDTO product = productClient.getProductById(productId);
        Integer stockAvailable = product.getStockQuantity();

        // Tim gio hang theo customerId
        // Trong hàm getCartByCustomerId và addToCart, sửa lại đoạn orElseGet:
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomerId(customerId);
                    newCart.setCartItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });

        // Tim trong gio hang co nhung san pham nao?
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        int currentInCart = existingItem.map(CartItem::getQuantity).orElse(0);
        int totalWanted = currentInCart + quantity;

        if (totalWanted > product.getStockQuantity()) {
            throw new RuntimeException("Kho chỉ còn " + product.getStockQuantity() + " sản phẩm!");
        }

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
        // 1. Check kho trước khi cho cập nhật (Tránh việc khách sửa 1 thành 100 trong giỏ)
        ProductDTO product = productClient.getProductById(productId);
        if (newQuantity > product.getStockQuantity()) {
            throw new RuntimeException("Không thể cập nhật. Kho chỉ còn " + product.getStockQuantity() + " sản phẩm!");
        }

        // 2. Tim gio hang cua customerId
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));

        // 3. Tim san pham trong gio hang va cap nhat so luong moi
        CartItem itemToUpdate = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng"));

        itemToUpdate.setQuantity(newQuantity);

        // 4. Luu và tra ve ket qua
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
                            .productImage(dto != null && dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()
                                    ? dto.getImageUrls().get(0) // Lấy ảnh đầu tiên
                                    : null)
                            .unitPrice(price)
                            .quantity(item.getQuantity())
                            .subTotal(price * item.getQuantity())
                            .build();
                })
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomerId())
                .createdAt(cart.getCreatedAt()) // 2. Thêm thời gian từ BaseEntity
                .updatedAt(cart.getUpdatedAt())
                .cartItemsResponse(itemResponses)
                .totalPrice(itemResponses.stream().mapToDouble(CartResponse.CartItemResponse::getSubTotal).sum())
                .totalItems(itemResponses.stream().mapToInt(CartResponse.CartItemResponse::getQuantity).sum())
                .build();
    }
}
