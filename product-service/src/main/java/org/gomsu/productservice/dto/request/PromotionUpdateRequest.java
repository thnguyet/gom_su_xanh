package org.gomsu.productservice.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
@Data
public class PromotionUpdateRequest {
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<PromotionRequest.ProductDiscountRequest> productDiscounts;
}
