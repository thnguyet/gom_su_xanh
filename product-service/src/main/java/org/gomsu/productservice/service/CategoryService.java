package org.gomsu.productservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.productservice.dto.request.CategoryCreationRequest;
import org.gomsu.productservice.dto.request.CategoryUpdateRequest;
import org.gomsu.productservice.dto.response.CategoryResponse;
import org.gomsu.productservice.entity.Category;
import org.gomsu.productservice.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // Lay danh sach toan bo Category
    public Page<CategoryResponse> getCategoryList(int page, int size, String keyword, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Category> categoryPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            categoryPage = categoryRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        }
        else {
            categoryPage = categoryRepository.findAll(pageable);
        }
        return categoryPage.map(this::toCategoryResponse);
    }

    // Xem bo suu tap theo id
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));
        return toCategoryResponse(category);
    }

    // Tao Category moi
    public CategoryResponse createCategory(CategoryCreationRequest categoryCreationRequest) {
        Category category = new Category();
        category.setName(categoryCreationRequest.getName().trim());
        return toCategoryResponse(categoryRepository.save(category));
    }

    // Sua Category
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest categoryUpdateRequest) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));
        category.setName(categoryUpdateRequest.getName().trim());
        return toCategoryResponse(categoryRepository.save(category));
    }

    // Xoa Category
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));
        if (category.getProductCount() != null && category.getProductCount() > 0) {
            throw new RuntimeException("Không thể xóa! Danh mục này đang chứa " + category.getProductCount() + " sản phẩm.");
        }
        categoryRepository.delete(category);
    }

    public CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .productCount(category.getProductCount() != null ? category.getProductCount() : 0)
                .build();
    }
}
