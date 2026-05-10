package org.gomsu.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationRequest {
    @NotBlank(message = "Email hoặc Số điện thoại không được để trống!")
    @Pattern(
            regexp = "^([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}|0\\d{9})$",
            message = "Email hoặc Số điện thoại không đúng định dạng!"
    )
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống!")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message = "Mật khẩu phải gồm ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt")
    private String password;
}
