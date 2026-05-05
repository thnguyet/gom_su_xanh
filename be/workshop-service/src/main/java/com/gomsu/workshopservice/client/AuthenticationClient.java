package com.gomsu.workshopservice.client;

import com.gomsu.workshopservice.dto.request.IntrospectRequest;
import com.gomsu.workshopservice.dto.response.IntrospectResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "identity-service", contextId = "authClient")
public interface AuthenticationClient {
    @PostMapping(value = "/auth/introspect", consumes = MediaType.APPLICATION_JSON_VALUE)
    IntrospectResponse introspect(@RequestBody IntrospectRequest request);
}
