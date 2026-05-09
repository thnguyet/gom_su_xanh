package org.gomsu.orderservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    // Order errors (3xxx)
    ORDER_NOT_FOUND(3001, "Không tìm thấy đơn hàng!", HttpStatus.NOT_FOUND),
    ORDER_EMPTY_SELECTION(3002, "Vui lòng chọn ít nhất một sản phẩm để đặt hàng!", HttpStatus.BAD_REQUEST),
    ORDER_MISSING_ADDRESS(3003, "Địa chỉ nhận hàng không được để trống!", HttpStatus.BAD_REQUEST),
    ORDER_CANCEL_UNAUTHORIZED(3004, "Bạn không có quyền hủy đơn hàng này!", HttpStatus.FORBIDDEN),
    ORDER_CANCEL_INVALID_STATUS(3005, "Đơn hàng đang trong quá trình vận chuyển, không thể hủy!", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_CANCELLED(3006, "Đơn hàng đã hủy không thể cập nhật trạng thái khác!", HttpStatus.BAD_REQUEST),

    // Cart errors (31xx)
    CART_ITEM_NOT_FOUND(3101, "Sản phẩm không có trong giỏ hàng", HttpStatus.NOT_FOUND),
    CART_INSUFFICIENT_STOCK(3102, "Kho hàng không đủ số lượng!", HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY(3103, "Số lượng sản phẩm phải lớn hơn 0!", HttpStatus.BAD_REQUEST),

    // Shipping & Payment errors (32xx)
    SHIPPING_METHOD_NOT_FOUND(3201, "Không tìm thấy đơn vị vận chuyển!", HttpStatus.NOT_FOUND),
    PAYMENT_METHOD_NOT_FOUND(3202, "Không tìm thấy phương thức thanh toán!", HttpStatus.NOT_FOUND),
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
