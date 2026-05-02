package org.gomsu.orderservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class ShippingMethodUpdateRequest {
    private String name; // Không có @NotBlank, có thể null

    @DecimalMin(value = "0.0")
    private Double shippingFee; // Có thể null

    private Boolean active;
}