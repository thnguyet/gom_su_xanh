package org.gomsu.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodRequest {

    @NotBlank(message = "Tên phương thức thanh toán không được để trống!")
    @Size(min = 2, max = 50, message = "Tên phương thức thanh toán phải từ 2 đến 50 ký tự")
    private String name;
}