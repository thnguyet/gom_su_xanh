package org.gomsu.identityservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;

    private String username;

    private String phone;

    private String address;

    private String email;

    private String gender;

    private String roles; //1 nguoi chi co 1 role duy nhat. Hoac ADMIN hoac USER
}
