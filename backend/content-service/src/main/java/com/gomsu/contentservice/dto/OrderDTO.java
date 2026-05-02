package com.gomsu.contentservice.dto;

import lombok.Data;

@Data
public class OrderDTO {
    private Long id;
    private Long customerId;
    private String status;      // Sẽ nhận các giá trị: PENDING, SHIPPING, CANCELLED, COMPLETED...
    private Double totalAmount;
    // Nguyệt có thể thêm các trường khác nếu cần dùng để check logic
}