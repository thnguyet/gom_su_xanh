package org.gomsu.productservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionRequest {

    @NotBlank(message = "Tên chương trình khuyến mãi không được để trống!")
    @Size(min = 5, max = 100, message = "Tên chương trình phải từ 5 đến 100 ký tự")
    private String name;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @FutureOrPresent(message = "Ngày bắt đầu không thể ở quá khứ")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    @NotEmpty(message = "Danh sách sản phẩm giảm giá không được để trống")
    @Valid // Quan trọng: Để Java validate tiếp các object con bên trong List
    private List<ProductDiscountRequest> productDiscounts;

    @Data
    public static class ProductDiscountRequest {

        @NotNull(message = "ID sản phẩm không được để trống")
        private Long productId;

        @NotNull(message = "Phần trăm giảm giá không được để trống")
        @DecimalMin(value = "0.0", message = "Giảm giá thấp nhất là 0%")
        @DecimalMax(value = "100.0", message = "Giảm giá tối đa là 100%")
        private Double discountPercentage;
    }
}