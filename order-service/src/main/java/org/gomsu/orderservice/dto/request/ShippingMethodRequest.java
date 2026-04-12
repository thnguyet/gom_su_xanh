package org.gomsu.orderservice.dto.request;

import lombok.Data;

@Data
public class ShippingMethodRequest {
    private String name;
    private Double shippingFee;
}
