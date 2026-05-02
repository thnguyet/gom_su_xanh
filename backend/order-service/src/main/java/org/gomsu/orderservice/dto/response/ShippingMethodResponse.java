package org.gomsu.orderservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShippingMethodResponse {
    private Long id;
    private String name;
    private Double shippingFee;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
