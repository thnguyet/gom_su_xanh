package org.gomsu.productservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private Double price;
    private String description;
    private String brand;
    private Integer stockQuantity;

    // Thay vì trả về cả object Category, ta chỉ cần tên của nó
    private String categoryName;

    // Thay vì trả về list object ProductImage phức tạp, ta chỉ trả về list URL ảnh dạng chuỗi
    private List<String> imageUrls;

    // Nếu sản phẩm đang được giảm giá, có thể trả thêm trường này (hiển thị % giảm)
    private Double discountPercentage;
}
