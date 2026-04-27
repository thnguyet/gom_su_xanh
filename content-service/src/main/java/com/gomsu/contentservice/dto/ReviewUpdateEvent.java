package com.gomsu.contentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewUpdateEvent implements Serializable {
    private Long productId;
    private Double averageRating;
    private Long reviewCount;
}
