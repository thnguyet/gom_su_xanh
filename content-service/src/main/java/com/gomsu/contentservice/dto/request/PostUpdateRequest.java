package com.gomsu.contentservice.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class PostUpdateRequest {
    private String title;
    private String content;
    private String summary;
    private Long categoryId;
    private Boolean published;

    // QUAN TRỌNG: Danh sách ID của những ảnh cũ mà người dùng muốn xóa
    private List<Long> deletedImageIds;
}
