package org.gomsu.productservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponse {
    private Long id;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<ProductPromotionItemResponse> items;

    @Data
    @Builder
    public static class ProductPromotionItemResponse {
        private Long productId;
        private String productName;
        private Double discountPercentage;
        private Double originalPrice;
        private Double discountedPrice;
    }
}
