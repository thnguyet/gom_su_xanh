package com.gomsu.workshopservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    // Workshop errors (2xxx)
    WORKSHOP_NOT_FOUND(2001, "Không tìm thấy Workshop!", HttpStatus.NOT_FOUND),
    WORKSHOP_INACTIVE(2002, "Workshop này hiện không còn hoạt động hoặc đã bị đóng!", HttpStatus.BAD_REQUEST),
    WORKSHOP_IMAGE_UPLOAD_FAILED(2003, "Lỗi khi upload ảnh", HttpStatus.INTERNAL_SERVER_ERROR),

    // Registration errors (21xx)
    REGISTRATION_NOT_FOUND(2101, "Không tìm thấy đơn đăng ký!", HttpStatus.NOT_FOUND),
    REGISTRATION_INVALID_QUANTITY(2102, "Số lượng vé phải lớn hơn 0!", HttpStatus.BAD_REQUEST),
    REGISTRATION_TIME_INVALID(2103, "Thời gian đăng ký không hợp lệ (chưa mở hoặc đã kết thúc)!", HttpStatus.BAD_REQUEST),
    REGISTRATION_OUT_OF_SLOTS(2104, "Workshop đã hết chỗ hoặc số lượng đăng ký vượt quá giới hạn!", HttpStatus.BAD_REQUEST),
    REGISTRATION_USER_NOT_FOUND(2105, "Không tìm thấy thông tin người dùng từ Identity Service!", HttpStatus.BAD_REQUEST),
    REGISTRATION_INVALID_PHONE(2106, "Số điện thoại không đúng định dạng!", HttpStatus.BAD_REQUEST),
    REGISTRATION_UNAUTHORIZED(2107, "Bạn không có quyền hủy đơn đăng ký này!", HttpStatus.FORBIDDEN),
    REGISTRATION_ALREADY_CANCELLED(2108, "Đơn hàng này đã bị hủy hoặc đã hoàn thành, không thể hủy thêm.", HttpStatus.BAD_REQUEST),
    REGISTRATION_CANCEL_DEADLINE(2109, "Đã quá hạn hủy vé! Bạn chỉ có thể hủy trước ngày diễn ra ít nhất 3 ngày.", HttpStatus.BAD_REQUEST),
    REGISTRATION_REFUND_FAILED(2110, "Có lỗi xảy ra khi hoàn trả số lượng vé!", HttpStatus.INTERNAL_SERVER_ERROR),
    REGISTRATION_INVALID_CHECKIN(2111, "Đơn đăng ký không hợp lệ để check-in", HttpStatus.BAD_REQUEST),
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
