package org.gomsu.productservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gomsu.productservice.dto.request.CategoryCreationRequest;
import org.gomsu.productservice.dto.request.CategoryUpdateRequest;
import org.gomsu.productservice.dto.response.CategoryResponse;
import org.gomsu.productservice.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    final CategoryService categoryService;

    // Xem toan bo category
    @GetMapping("/all")
    public ResponseEntity<Page<CategoryResponse>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    )
    {
        return ResponseEntity.ok(categoryService.getCategoryList(page, size, keyword, sortBy, sortDir));
    }

    // Xem bo suu tap theo id
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    // Tao Category moi
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestBody @Valid CategoryCreationRequest  categoryCreationRequest
            ) {
        return ResponseEntity.ok(categoryService.createCategory(categoryCreationRequest));
    }

    // Sua Category
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid CategoryUpdateRequest categoryUpdateRequest
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(id, categoryUpdateRequest));
    }

    // Xoa Category
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteCategory(
            @PathVariable Long id
    ) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Xóa thành công!");
    }
}
