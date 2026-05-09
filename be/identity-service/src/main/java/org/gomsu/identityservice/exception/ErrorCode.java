package org.gomsu.identityservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNAUTHENTICATED(1006, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    INVALID_KEY(1001, "Mã lỗi không hợp lệ", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "Người dùng đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Tên người dùng phải có ít nhất 3 ký tự", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Mật khẩu phải có ít nhất 8 ký tự", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "Tài khoản không tồn tại!", HttpStatus.NOT_FOUND),
    UNAUTHORIZED(1007, "Bạn không có quyền truy cập", HttpStatus.FORBIDDEN),

    // Authentication errors (10xx)
    WRONG_PASSWORD(1008, "Sai mật khẩu!", HttpStatus.BAD_REQUEST),
    TOKEN_GENERATION_FAILED(1009, "Lỗi tạo token", HttpStatus.INTERNAL_SERVER_ERROR),
    OAUTH2_EMAIL_NOT_FOUND(1010, "Không tìm thấy email từ tài khoản Google!", HttpStatus.BAD_REQUEST),

    // Registration & OTP errors (11xx)
    INVALID_OTP(1101, "Mã OTP không đúng hoặc đã hết hạn!", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_REGISTERED(1102, "Email này đã được đăng ký!", HttpStatus.BAD_REQUEST),
    PHONE_ALREADY_REGISTERED(1103, "Số điện thoại này đã được đăng ký!", HttpStatus.BAD_REQUEST),
    EMAIL_TAKEN_BY_OTHER(1104, "Email mới đã được đăng ký bởi tài khoản khác!", HttpStatus.BAD_REQUEST),

    // Password errors (12xx)
    OLD_PASSWORD_WRONG(1201, "Mật khẩu không đúng!", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_SAME_AS_OLD(1202, "Mật khẩu mới không được trùng với mật khẩu hiện tại!", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_FOUND_RESET(1203, "Email không tồn tại trong hệ thống!", HttpStatus.NOT_FOUND),
    PASSWORDS_NOT_MATCHED(1204, "Mật khẩu xác nhận không khớp!", HttpStatus.BAD_REQUEST),
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
