package org.gomsu.orderservice.client;

import org.gomsu.orderservice.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service", contextId = "identityUserClient")
public interface UserClient {
    @GetMapping("/users/my-infor")
    UserResponse getMyInfor();
}
