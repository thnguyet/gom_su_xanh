package com.gomsu.contentservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryUpdateRequest {
    @Size(max = 100, message = "Tên danh mục không được quá 100 ký tự")
    private String name;

    private String description;

    // Ví dụ sau này Nguyệt muốn thêm ẩn/hiện danh mục
    private Boolean active;
}