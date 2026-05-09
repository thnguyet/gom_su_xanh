package org.gomsu.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordChangeRequest {
    @NotBlank(message = "Mật khẩu cũ không được để trống!")
    private String oldPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống!")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(regexp = ".*[A-Z].*", message = "Mật khẩu phải có ít nhất một chữ cái viết hoa")
    @Pattern(regexp = ".*[a-z].*", message = "Mật khẩu phải có ít nhất một chữ cái viết thường")
    @Pattern(regexp = ".*[0-9].*", message = "Mật khẩu phải có ít nhất một chữ số")
    @Pattern(regexp = ".*[@#$%^&+=!].*", message = "Mật khẩu phải có ít nhất một ký tự đặc biệt (@#$%^&+=!)")
    @Pattern(regexp = "^\\S+$", message = "Mật khẩu không được chứa khoảng trắng")
    private String newPassword;

    @NotBlank(message = "Mật khẩu xác nhận không được để trống!")
    private String confirmPassword;
}
