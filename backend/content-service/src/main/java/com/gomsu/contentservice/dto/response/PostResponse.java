package com.gomsu.contentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostResponse {
    private Long id;
    private String title;
    private String slug; // Cực kỳ quan trọng để FE làm URL
    private String content;
    private String summary;
    private String thumbnail;

    private List<String> images; // Danh sách tất cả các ảnh chi tiết của bài viết

    // Trả về thông tin category để hiện "Tin tức" hay "Kiến thức"
    private CategoryResponse category;

    private Long authorId;
    private LocalDateTime createdAt; // Để hiện "Đăng ngày 23/04/2026"

    @JsonProperty("isPublished")
    private boolean published;
}
