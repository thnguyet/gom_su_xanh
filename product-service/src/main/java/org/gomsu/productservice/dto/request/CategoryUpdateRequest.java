package org.gomsu.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jdk.jfr.Name;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryUpdateRequest {
    @NotBlank(message = "Không được để trống tên danh mục")
    private String name;
}
