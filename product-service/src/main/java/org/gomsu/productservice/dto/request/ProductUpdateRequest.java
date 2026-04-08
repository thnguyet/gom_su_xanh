package org.gomsu.productservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateRequest {
    private String name;

    @Min(value = 0, message = "Gía sản phẩm phải lớn hơn hoặc bằng 0")
    private Double price;

    private String description;

    private String brand;

    @Min(value = 0, message = "Số lượng tồn kho không hợp lệ")
    private Integer stockQuantity;

    private Long categoryId;

    private List<Long> deletedImageIds;
}
