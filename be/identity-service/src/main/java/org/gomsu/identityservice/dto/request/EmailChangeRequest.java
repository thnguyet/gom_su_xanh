package org.gomsu.identityservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailChangeRequest {
    @NotBlank(message = "Email mới không được để trống!")
    @Email(message = "Nhập đúng định dạng email!")
    private String newEmail;

    @NotBlank(message = "Mã OTP không được để trống!")
    private String otp;
}
