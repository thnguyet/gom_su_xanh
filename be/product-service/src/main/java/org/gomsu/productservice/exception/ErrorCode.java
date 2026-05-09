package org.gomsu.productservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    // Product errors (4xxx)
    PRODUCT_NOT_FOUND(4001, "Không tìm thấy sản phẩm!", HttpStatus.NOT_FOUND),
    PRODUCT_IMAGE_UPLOAD_FAILED(4002, "Lỗi upload ảnh", HttpStatus.INTERNAL_SERVER_ERROR),
    PRODUCT_IMAGE_DELETE_FAILED(4003, "Lỗi khi xóa ảnh trên Cloudinary", HttpStatus.INTERNAL_SERVER_ERROR),
    PRODUCT_INSUFFICIENT_STOCK(4004, "Sản phẩm đã hết hàng hoặc không đủ số lượng!", HttpStatus.BAD_REQUEST),

    // Category errors (41xx)
    CATEGORY_NOT_FOUND(4101, "Không tìm thấy danh mục!", HttpStatus.NOT_FOUND),
    CATEGORY_NAME_EXISTS(4102, "Tên danh mục này đã tồn tại!", HttpStatus.BAD_REQUEST),
    CATEGORY_SLUG_EXISTS(4103, "Đường dẫn (Slug) này đã tồn tại!", HttpStatus.BAD_REQUEST),
    CATEGORY_IMAGE_UPLOAD_FAILED(4104, "Lỗi upload ảnh danh mục!", HttpStatus.INTERNAL_SERVER_ERROR),
    CATEGORY_HAS_PRODUCTS(4105, "Không thể xóa danh mục đang chứa sản phẩm!", HttpStatus.BAD_REQUEST),

    // Promotion errors (42xx)
    PROMOTION_NOT_FOUND(4201, "Không tìm thấy đợt khuyến mãi!", HttpStatus.NOT_FOUND),
    PROMOTION_ALREADY_EXISTS(4202, "Chương trình khuyến mãi này đã tồn tại rồi!", HttpStatus.BAD_REQUEST),
    PROMOTION_ALREADY_STOPPED(4203, "Chương trình này đã tạm dừng từ trước đó.", HttpStatus.BAD_REQUEST),
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
