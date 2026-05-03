package com.gomsu.contentservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateRequest {

    @Min(value = 1, message = "Đánh giá thấp nhất là 1 sao")
    @Max(value = 5, message = "Đánh giá cao nhất là 5 sao")
    private Integer rating; // Có thể null nếu không đổi sao

    @Size(max = 1000, message = "Nội dung đánh giá không quá 1000 ký tự")
    private String comment; // Có thể null nếu không đổi nội dung

    // Lưu ý: productId và orderId KHÔNG nên cho update
    // vì đánh giá đã gắn chặt với sản phẩm và đơn hàng đó rồi.
}