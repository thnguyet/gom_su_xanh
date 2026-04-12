package org.gomsu.orderservice.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    private String address;      // Địa chỉ giao hàng
    private String phoneNumber;  // Số điện thoại nhận hàng
    private String note;         // Ghi chú đơn hàng
    private Long paymentMethodId; // ID của phương thức thanh toán khách chọn
    private Long shippingMethodId; // ID của đơn vị vận chuyển

    private List<Long> selectedCartItemIds;
}
