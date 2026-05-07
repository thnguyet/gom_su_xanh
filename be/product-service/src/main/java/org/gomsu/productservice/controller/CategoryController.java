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
     * DÀNH CHO DROPDOWN: Lấy danh sách danh mục đang hoạt động (không phân trang)
     */
    @GetMapping("/active")
    public ResponseEntity<java.util.List<CategoryResponse>> getActiveCategories() {
        return ResponseEntity.ok(categoryService.getActiveCategories());
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
     * DÀNH CHO ADMIN: Tạo danh mục mới (Hỗ trợ upload ảnh)
     */
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestPart("request") @Valid CategoryCreationRequest request,
            @RequestPart(value = "image", required = false) org.springframework.web.multipart.MultipartFile image
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request, image));
    }

    /**
     * DÀNH CHO ADMIN: Cập nhật danh mục (Hỗ trợ upload ảnh & trạng thái)
     */
    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @RequestPart("request") @Valid CategoryUpdateRequest request,
            @RequestPart(value = "image", required = false) org.springframework.web.multipart.MultipartFile image
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request, image));
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