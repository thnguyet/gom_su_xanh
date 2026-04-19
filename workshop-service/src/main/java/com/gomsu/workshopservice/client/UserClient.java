package com.gomsu.workshopservice.client;

import com.gomsu.workshopservice.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service", contextId = "userClient")
public interface UserClient {
    @GetMapping("/users/{id}")
    UserResponse getUserById(@PathVariable Long id);
}
