package org.gomsu.orderservice.client;

import org.gomsu.orderservice.dto.ProductDTO;
import org.gomsu.orderservice.dto.request.ProductRestockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service", contextId = "productClient")
public interface ProductClient {
    @GetMapping("/products/{id}")
    ProductDTO getProductById(@RequestParam("id") Long id);

    @GetMapping("products/get-by-ids")
    List<ProductDTO> getProductsByIds(@RequestParam("ids") List<Long> ids);

    @PutMapping("/products/reduce-stock") // Nhớ khớp với Path bên ProductController
    void reduceStock(@RequestBody List<ProductRestockRequest> requests);
}
