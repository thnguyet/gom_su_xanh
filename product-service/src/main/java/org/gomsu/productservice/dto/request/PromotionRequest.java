package org.gomsu.productservice.dto.request;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionRequest {
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Danh sách các sản phẩm được áp dụng khuyến mãi
    private List<ProductDiscountRequest> productDiscounts;

    @Data
    public static class ProductDiscountRequest {
        private Long productId;
        private Double discountPercentage; // Giảm riêng cho từng sản phẩm
    }
}