package com.gomsu.contentservice.client;

import com.gomsu.contentservice.configuration.FeignConfig;
import com.gomsu.contentservice.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", configuration = FeignConfig.class)
public interface OrderClient {
    @GetMapping("/orders/{id}")
    OrderDTO getOrderById(@PathVariable("id") Long id);
}
