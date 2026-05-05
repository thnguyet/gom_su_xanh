package com.gomsu.contentservice.client;

import com.gomsu.contentservice.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", contextId = "productClient") // Tên service của Nguyệt trên Eureka
public interface ProductClient {
    @GetMapping("/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);
}
