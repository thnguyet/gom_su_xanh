package org.gomsu.orderservice.client;

import org.gomsu.orderservice.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/products/{id}")
    ProductDTO getProductById(@RequestParam("id") Long id);

    @GetMapping("products/get-by-ids")
    List<ProductDTO> getProductsByIds(@RequestParam("ids") List<Long> ids);
}
