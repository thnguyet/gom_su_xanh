package com.gomsu.contentservice.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class ReviewSummaryResponse {
    private Long productId;
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> starCounts; // Lưu số lượng: {5: 100, 4: 20, ...}
}