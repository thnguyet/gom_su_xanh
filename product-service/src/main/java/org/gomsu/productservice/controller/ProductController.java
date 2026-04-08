package org.gomsu.productservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gomsu.productservice.dto.request.ProductCreationRequest;
import org.gomsu.productservice.dto.request.ProductUpdateRequest;
import org.gomsu.productservice.dto.response.ProductResponse;
import org.gomsu.productservice.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED) // Trả về mã 201 (Đã tạo thành công)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse createProduct(

            // @RequestPart("data") hứng cục chữ (JSON) và @Valid để kiểm tra dữ liệu không được bỏ trống
            @RequestPart("data") @Valid ProductCreationRequest request,

            // @RequestPart("images") hứng danh sách file ảnh tải lên
            @RequestPart(value = "images", required = false) List<MultipartFile> images

    ) {
        // Ném toàn bộ cho Service xử lý và trả về kết quả cho Frontend
        return productService.createProduct(request, images);
    }


    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestPart("data") @Valid ProductUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ResponseEntity.ok(productService.updateProduct(request, id, images));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Xóa thành công!");
    }

    @GetMapping("/all")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return ResponseEntity.ok(productService.getAllProducts(page, size, keyword, sortBy, sortDir));
    }

    @GetMapping("/get-by-ids")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<ProductResponse>> getProductsByIds(@RequestParam List<Long> ids) {
        // Gọi xuống Service để lấy danh sách sản phẩm tương ứng với list IDs
        return ResponseEntity.ok(productService.getProductsByIds(ids));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> uploadProductImage(@PathVariable Long id, @RequestPart(value = "images") List<MultipartFile> images) {
        return ResponseEntity.ok(productService.uploadProductImages(id, images));
    }
}
