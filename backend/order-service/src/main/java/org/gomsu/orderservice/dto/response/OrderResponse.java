package org.gomsu.orderservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gomsu.orderservice.entity.OrderDetail;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;

    private Double totalAmount;
    private String address;
    private String phoneNumber;
    private String note;
    private String paymentMethod;
    private String shippingMethod;
    private List<OrderDetailResponse> orderDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetailResponse {
        private Long productId;
        private String productName;
        private Double priceAtPurchase;
        private Integer quantity;
        private Double subTotal; // priceAtPurchase * quantity
    }
}
