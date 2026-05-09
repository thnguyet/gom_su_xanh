package com.gomsu.contentservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED(1006, "Xác thực người dùng thất bại", HttpStatus.UNAUTHORIZED),

    // Post errors (5xxx)
    POST_NOT_FOUND(5001, "Không tìm thấy bài viết!", HttpStatus.NOT_FOUND),
    POST_CATEGORY_NOT_FOUND(5002, "Không tìm thấy thể loại bài viết!", HttpStatus.NOT_FOUND),
    POST_CATEGORY_INACTIVE(5003, "Không thể tạo bài viết trong danh mục đã bị ẩn hoặc ngừng hoạt động!", HttpStatus.BAD_REQUEST),
    POST_IMAGE_UPLOAD_FAILED(5004, "Lỗi xử lý hình ảnh", HttpStatus.INTERNAL_SERVER_ERROR),
    POST_IMAGE_UPLOAD_NEW_FAILED(5005, "Lỗi khi upload ảnh mới", HttpStatus.INTERNAL_SERVER_ERROR),

    // Category errors (51xx)
    CATEGORY_NOT_FOUND(5101, "Không tìm thấy danh mục!", HttpStatus.NOT_FOUND),
    CATEGORY_ALREADY_EXISTS(5102, "Danh mục đã tồn tại!", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_POSTS(5103, "Không thể xóa danh mục đang chứa bài viết!", HttpStatus.BAD_REQUEST),

    // Review errors (52xx)
    REVIEW_NOT_FOUND(5201, "Đánh giá không tồn tại", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS(5202, "Bạn đã đánh giá sản phẩm này rồi!", HttpStatus.BAD_REQUEST),
    REVIEW_UNAUTHORIZED(5203, "Bạn không có quyền thực hiện hành động này!", HttpStatus.FORBIDDEN),
    REVIEW_EDIT_UNAUTHORIZED(5204, "Bạn không có quyền sửa!", HttpStatus.FORBIDDEN),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
