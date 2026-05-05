package com.gomsu.contentservice.controller;

import com.gomsu.contentservice.dto.request.CategoryRequest;
import com.gomsu.contentservice.dto.request.CategoryUpdateRequest;
import com.gomsu.contentservice.dto.response.CategoryResponse;
import com.gomsu.contentservice.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    // THÀNH CÔNG
    // Public cho khách xem
    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> getCategories(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Boolean isAdmin = false;

        if (jwt != null) {
            // Lấy quyền từ token để xác định là User hay Admin
            String scope = jwt.getClaimAsString("scope");
            log.info(">>> Truy cập danh mục - Scope: [{}]", scope);

            if (scope != null && scope.contains("ADMIN")) {
                isAdmin = true;
            }
        }

        // Gọi service xử lý logic "cái này cái kia" như Nguyệt muốn
        Page<CategoryResponse> response = categoryService.getCategories(
                keyword, isAdmin, active, page, size, sortBy, sortDir);

        return ResponseEntity.ok(response);
    }

    // THÀNH CÔNG
    // 1. Dành cho khách (Dùng Slug)
    @GetMapping("/detail/{slug}")
    public ResponseEntity<CategoryResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getBySlug(slug));
    }

    // THÀNH CÔNG
    // 2. Dành cho Admin (Dùng ID)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    // THÀNH CÔNG
    // Chỉ Admin mới được can thiệp dữ liệu
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(@RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    // THÀNH CÔNG
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
                                                   @RequestBody @Valid CategoryUpdateRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    // THÀNH CÔNG
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}