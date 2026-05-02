package org.gomsu.productservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gomsu.productservice.dto.request.CategoryCreationRequest;
import org.gomsu.productservice.dto.request.CategoryUpdateRequest;
import org.gomsu.productservice.dto.response.CategoryResponse;
import org.gomsu.productservice.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * DÀNH CHO ADMIN & USER: Lấy danh sách danh mục (có phân trang & tìm kiếm) (THÀNH CÔNG)
     */
    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(categoryService.getCategoryList(page, size, keyword, sortBy, sortDir));
    }

    /**
     * DÀNH CHO USER: Lấy chi tiết danh mục theo SLUG (Để làm trang bộ sưu tập)
     * Ví dụ: GET /categories/detail/am-chen-tu-sa (THÀNH CÔNG)
     */
    @GetMapping("/detail/{slug}")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getCategoryBySlug(slug));
    }

    /**
     * DÀNH CHO HỆ THỐNG/ADMIN: Lấy chi tiết theo ID (THÀNH CÔNG)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    /**
     * DÀNH CHO ADMIN: Tạo danh mục mới (THÀNH CÔNG)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestBody @Valid CategoryCreationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request));
    }

    /**
     * DÀNH CHO ADMIN: Cập nhật danh mục (THÀNH CÔNG)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid CategoryUpdateRequest request
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    /**
     * DÀNH CHO ADMIN: Xóa danh mục (THÀNH CÔNG)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Đã xóa danh mục gốm sứ thành công!");
    }
}