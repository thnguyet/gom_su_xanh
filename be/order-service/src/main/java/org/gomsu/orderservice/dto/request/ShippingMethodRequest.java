package org.gomsu.orderservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingMethodRequest {

    @NotBlank(message = "Tên đơn vị vận chuyển không được để trống!")
    @Size(min = 2, max = 100, message = "Tên đơn vị vận chuyển phải từ 2 đến 100 ký tự")
    private String name; // Ví dụ: Giao hàng nhanh (GHN), Giao hàng tiết kiệm (GHTK)

    @NotNull(message = "Phí vận chuyển không được để trống!")
    @DecimalMin(value = "0.0", message = "Phí vận chuyển không được nhỏ hơn 0!")
    private Double shippingFee;
}