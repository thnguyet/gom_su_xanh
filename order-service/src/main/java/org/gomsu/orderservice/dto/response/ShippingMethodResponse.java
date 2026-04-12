package org.gomsu.orderservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShippingMethodResponse {
    private Long id;
    private String name;
    private Double shippingFee;
}
