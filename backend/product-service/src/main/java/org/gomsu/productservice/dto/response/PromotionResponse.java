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
    private String slug;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean isActive; // Để FE hiển thị nhãn "Đang chạy" hoặc "Tạm dừng"

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductPromotionItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor // Thêm để tránh lỗi Jackson khi mapping
    @AllArgsConstructor
    public static class ProductPromotionItemResponse {
        private Long productId;
        private String productName;
        private String productSlug; // Thêm để User bấm vào tên sản phẩm là nhảy sang trang Detail
        private Double discountPercentage;
        private Double originalPrice;
        private Double discountedPrice;
    }
}
