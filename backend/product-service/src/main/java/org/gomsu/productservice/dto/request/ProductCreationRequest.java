package org.gomsu.productservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreationRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống!")
    private String name;

    @NotNull(message = "Gía không được để trống!")
    @Min(value = 0, message = "Gía sản phẩm phải lớn hơn hoặc bằng 0")
    private Double price;

    private String description;

    private String brand;

    @NotNull(message = "Số lượng không được để trống!")
    @Min(value = 0, message = "Số lượng tồn kho không hợp lệ")
    private Integer stockQuantity;

    @NotNull(message = "Mã danh mục không được để trống!")
    private Long categoryId;

    private java.util.List<String> imageUrls;
}
