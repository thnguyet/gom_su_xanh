package com.gomsu.contentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntrospectResponse {
    boolean valid; // Identity trả về true nếu token còn dùng được, false nếu đã logout/hết hạn
}
