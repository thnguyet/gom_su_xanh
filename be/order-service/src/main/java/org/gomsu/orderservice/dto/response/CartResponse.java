package org.gomsu.orderservice.dto.response;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartResponse {
    private Long id;
    private Long customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CartItemResponse> cartItemsResponse;

    private Integer totalItems; // tong so luong mat hang
    private Double totalPrice; // tong tien

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CartItemResponse {
        private Long productId;
        private String productName;
        private String productImage;
        private Double unitPrice;
        private Integer quantity;
        private Double subTotal; // Thanh tien: unitPrice * quantity
        private Integer stockQuantity;
    }
}
