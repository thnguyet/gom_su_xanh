package org.gomsu.productservice.repository.httpclient;

import org.gomsu.productservice.dto.request.IntrospectRequest;
import org.gomsu.productservice.dto.response.IntrospectResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "identity-service")
public interface AuthenticationClient {
    @PostMapping(value = "/auth/introspect", consumes = MediaType.APPLICATION_JSON_VALUE)
    IntrospectResponse introspect(@RequestBody IntrospectRequest request);
}