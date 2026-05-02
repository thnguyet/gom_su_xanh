package org.gomsu.orderservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @Size(max = 255, message = "Địa chỉ quá dài, vui lòng kiểm tra lại")
    private String address;

    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
            message = "Số điện thoại không đúng định dạng Việt Nam!")
    private String phoneNumber;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán!")
    private Long paymentMethodId;

    @NotNull(message = "Vui lòng chọn đơn vị vận chuyển!")
    private Long shippingMethodId;

    @NotEmpty(message = "Danh sách sản phẩm thanh toán không được để trống!")
    private List<Long> selectedCartItemIds;
}