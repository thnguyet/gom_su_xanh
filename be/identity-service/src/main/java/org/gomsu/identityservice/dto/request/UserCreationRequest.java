package org.gomsu.identityservice.dto.request;

import jakarta.validation.constraints.Email;
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
public class UserCreationRequest {
    @NotBlank(message = "Tên tài khoản không được để trống!")
    private String username;

    @NotBlank(message = "Số điện thoại không được để trống!")
    @Pattern(regexp = "^0\\d{9}", message = "Số điện thoại phải gồm 10 số và bắt đầu là số 0!")
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống!")
    private String address;

    @NotBlank(message = "Địa chỉ email không được để trống!")
    @Email(message = "Email phải đúng định dạng!")
    private String email;

    @NotBlank(message = "Giới tính không được để trống!")
    private String gender;

    @NotBlank(message = "Mật khẩu không được để trống!")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(regexp = ".*[A-Z].*", message = "Mật khẩu phải có ít nhất một chữ cái viết hoa")
    @Pattern(regexp = ".*[a-z].*", message = "Mật khẩu phải có ít nhất một chữ cái viết thường")
    @Pattern(regexp = ".*[0-9].*", message = "Mật khẩu phải có ít nhất một chữ số")
    @Pattern(regexp = ".*[@#$%^&+=!].*", message = "Mật khẩu phải có ít nhất một ký tự đặc biệt (@#$%^&+=!)")
    @Pattern(regexp = "^\\S+$", message = "Mật khẩu không được chứa khoảng trắng")
    private String password;
}
