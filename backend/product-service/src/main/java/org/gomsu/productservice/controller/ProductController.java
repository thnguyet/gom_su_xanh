package org.gomsu.productservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gomsu.productservice.dto.request.ProductCreationRequest;
import org.gomsu.productservice.dto.request.ProductRestockRequest;
import org.gomsu.productservice.dto.request.ProductUpdateRequest;
import org.gomsu.productservice.dto.response.ProductResponse;
import org.gomsu.productservice.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // 1. Thêm sản phẩm (Tích hợp Slug trong Service) (THÀNH CÔNG)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse createProduct(
            @RequestPart("data") @Valid ProductCreationRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return productService.createProduct(request, images);
    }

    // 2. Lấy toàn bộ sản phẩm (Cập nhật tham số Lọc thời gian) (THÀNH CÔNG)
    @GetMapping("/all")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt") String sortBy, // Mặc định xếp theo ngày tạo
            @RequestParam(defaultValue = "desc") String sortDir,      // Mặc định cái mới nhất lên đầu
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        return ResponseEntity.ok(productService.getAllProducts(page, size, keyword, sortBy, sortDir, fromDate, toDate));
    }

    // 3. Lấy chi tiết sản phẩm THEO SLUG (Dành cho trang khách hàng) (THÀNH CÔNG)
    // Ví dụ: GET /products/detail/binh-hoa-men-ran
    @GetMapping("/detail/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getProductBySlug(slug));
    }

    // 4. Cập nhật sản phẩm (THÀNH CÔNG)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestPart("data") @Valid ProductUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ResponseEntity.ok(productService.updateProduct(request, id, images));
    }

    // 5. Xóa sản phẩm (THÀNH CÔNG)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Xóa thành công sản phẩm gốm sứ!");
    }

    // 6. Các API bổ trợ khác (Giữ nguyên logic của Nguyệt)
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/get-by-ids")
    public ResponseEntity<List<ProductResponse>> getProductsByIds(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(productService.getProductsByIds(ids));
    }

    @PutMapping("/restock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> restock(@RequestBody List<ProductRestockRequest> requests) {
        productService.restockProducts(requests);
        return ResponseEntity.ok("Hoàn kho thành công!");
    }

    @PutMapping("/reduce-stock")
    public ResponseEntity<Void> reduceStock(@RequestBody List<ProductRestockRequest> requests) {
        productService.reduceStock(requests);
        return ResponseEntity.ok().build();
    }
}
