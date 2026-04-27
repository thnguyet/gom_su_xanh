package com.gomsu.contentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class PostRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 10, max = 200, message = "Tiêu đề phải từ 10 đến 200 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private String summary; // Mô tả ngắn

    @NotNull(message = "Thể loại bài viết là bắt buộc")
    private Long categoryId; // Chỉ cần truyền ID thể loại lên là đủ

    private boolean published; // Có đăng ngay hay không

    private List<MultipartFile> images;
}
