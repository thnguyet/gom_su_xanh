package org.gomsu.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewUpdateEvent {
    private Long productId;
    private Double averageRating;
    private Long reviewCount;
}
