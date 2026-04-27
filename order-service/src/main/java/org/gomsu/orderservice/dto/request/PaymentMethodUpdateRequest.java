package org.gomsu.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentMethodUpdateRequest {
    @Size(min = 2, max = 50, message = "Tên phương thức thanh toán phải từ 2 đến 50 ký tự")
    private String name;
    private Boolean active;
}
