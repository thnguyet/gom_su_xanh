package org.gomsu.orderservice.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
public class PaymentMethodRequest {
    private String name;
}
