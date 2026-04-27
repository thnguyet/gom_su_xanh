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
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // Lấy danh sách (Nâng cấp mặc định xếp theo ngày tạo mới nhất)
    public Page<CategoryResponse> getCategoryList(int page, int size, String keyword, String sortBy, String sortDir) {
        // Nếu mặc định id, hãy đổi thành createdAt để thấy cái mới nhất
        String actualSortBy = sortBy.equals("id") ? "createdAt" : sortBy;

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(actualSortBy).ascending()
                : Sort.by(actualSortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Category> categoryPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            categoryPage = categoryRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            categoryPage = categoryRepository.findAll(pageable);
        }
        return categoryPage.map(this::toCategoryResponse);
    }

    // Xem theo slug (Để làm trang "Bộ sưu tập theo loại")
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
        return toCategoryResponse(category);
    }

    // Tạo mới
    @Transactional
    public CategoryResponse createCategory(CategoryCreationRequest request) {
        String name = request.getName().trim();
        String slug = toSlug(name);

        if (categoryRepository.existsByName(name)) {
            throw new RuntimeException("Tên danh mục này đã tồn tại rồi Nguyệt ơi!");
        }

        // Thêm dòng này để bảo vệ cột Unique Slug
        if (categoryRepository.existsBySlug(slug)) {
            throw new RuntimeException("Đường dẫn (Slug) này đã tồn tại, Nguyệt hãy đặt tên khác một chút nhé!");
        }

        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        return toCategoryResponse(categoryRepository.save(category));
    }

    // Cập nhật
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        String newName = request.getName().trim();
        String newSlug = toSlug(newName);

        // Check trùng tên
        if (!category.getName().equals(newName) && categoryRepository.existsByName(newName)) {
            throw new RuntimeException("Tên danh mục mới bị trùng rồi!");
        }

        // Check trùng slug (Trường hợp Admin đổi tên nhưng lại trùng slug với danh mục khác)
        if (!category.getSlug().equals(newSlug) && categoryRepository.existsBySlug(newSlug)) {
            throw new RuntimeException("Đường dẫn này bị trùng với danh mục khác mất rồi!");
        }

        category.setName(newName);
        category.setSlug(newSlug);

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

    // Xem bo suu tap theo id
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));
        return toCategoryResponse(category);
    }

    // Mapping Response
    public CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .productCount(category.getProductCount() != null ? category.getProductCount() : 0)
                .createdAt(category.getCreatedAt())
                .build();
    }

    // Dùng chung hàm toSlug Nguyệt đã viết ở ProductService
    private String toSlug(String title) {
        if (title == null || title.isBlank()) return "";
        String slug = title.toLowerCase().replaceAll("đ", "d");
        String normalized = Normalizer.normalize(slug, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
