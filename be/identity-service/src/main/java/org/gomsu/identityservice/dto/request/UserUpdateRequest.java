package org.gomsu.identityservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    private String username;

    @Pattern(regexp = "^0\\d{9}", message = "Số điện thoại phải gồm 10 số và bắt đầu là số 0!")
    private String phone;

    private String address;

    private String gender;
}
