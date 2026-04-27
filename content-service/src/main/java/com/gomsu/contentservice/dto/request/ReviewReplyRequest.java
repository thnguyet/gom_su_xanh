package com.gomsu.contentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewReplyRequest {
    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(max = 500, message = "Nội dung phản hồi không quá 500 ký tự")
    private String adminReply;
}