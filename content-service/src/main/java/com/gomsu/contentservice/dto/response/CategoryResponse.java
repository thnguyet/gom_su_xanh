package com.gomsu.contentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;

    private String name; // Tên thể loại: "Kiến thức gốm sứ"

    private String description; // Mô tả ngắn (nếu cần hiện ở trang danh mục)

    // Nếu Nguyệt muốn làm URL đẹp cho cả danh mục thì thêm slug ở đây
    private String slug;
}
