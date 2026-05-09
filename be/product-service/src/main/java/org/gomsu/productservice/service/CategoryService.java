package org.gomsu.productservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.productservice.dto.request.CategoryCreationRequest;
import org.gomsu.productservice.dto.request.CategoryUpdateRequest;
import org.gomsu.productservice.dto.response.CategoryResponse;
import org.gomsu.productservice.entity.Category;
import org.gomsu.productservice.exception.AppException;
import org.gomsu.productservice.exception.ErrorCode;
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
    private final CloudinaryService cloudinaryService;

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
            categoryPage = categoryRepository.findByIdOrName(keyword.trim(), pageable);
        } else {
            categoryPage = categoryRepository.findAll(pageable);
        }
        return categoryPage.map(this::toCategoryResponse);
    }

    // Xem theo slug (Để làm trang "Bộ sưu tập theo loại")
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return toCategoryResponse(category);
    }

    // Tạo mới
    @Transactional
    public CategoryResponse createCategory(CategoryCreationRequest request, org.springframework.web.multipart.MultipartFile image) {
        String name = request.getName().trim();
        String slug = toSlug(name);

        if (categoryRepository.existsByName(name)) {
            throw new AppException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        if (categoryRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.CATEGORY_SLUG_EXISTS);
        }

        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setActive(true);

        if (image != null && !image.isEmpty()) {
            try {
                String imageUrl = cloudinaryService.uploadImage(image);
                category.setImageUrl(imageUrl);
            } catch (java.io.IOException e) {
                throw new AppException(ErrorCode.CATEGORY_IMAGE_UPLOAD_FAILED);
            }
        }

        return toCategoryResponse(categoryRepository.save(category));
    }

    // Cập nhật
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request, org.springframework.web.multipart.MultipartFile image) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            String newSlug = toSlug(newName);

            if (!category.getName().equals(newName) && categoryRepository.existsByName(newName)) {
                throw new AppException(ErrorCode.CATEGORY_NAME_EXISTS);
            }
            category.setName(newName);
            category.setSlug(newSlug);
        }

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        if (Boolean.TRUE.equals(request.getDeleteImage())) {
            if (category.getImageUrl() != null) {
                try {
                    cloudinaryService.deleteImage(category.getImageUrl());
                } catch (Exception e) { /* Ignored */ }
                category.setImageUrl(null);
            }
        }

        if (image != null && !image.isEmpty()) {
            // Xóa ảnh cũ nếu có
            if (category.getImageUrl() != null) {
                try {
                    cloudinaryService.deleteImage(category.getImageUrl());
                } catch (Exception e) { /* Ignored */ }
            }
            try {
                String imageUrl = cloudinaryService.uploadImage(image);
                category.setImageUrl(imageUrl);
            } catch (java.io.IOException e) {
                throw new AppException(ErrorCode.CATEGORY_IMAGE_UPLOAD_FAILED);
            }
        }

        return toCategoryResponse(categoryRepository.save(category));
    }

    // Xoa Category
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        if (category.getProductCount() != null && category.getProductCount() > 0) {
            throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        
        if (category.getImageUrl() != null) {
            try {
                cloudinaryService.deleteImage(category.getImageUrl());
            } catch (Exception e) { /* Ignored */ }
        }
        
        categoryRepository.delete(category);
    }

    // Lấy tất cả danh mục đang hoạt động (không phân trang, dùng cho dropdown)
    public java.util.List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(this::toCategoryResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    // Xem bo suu tap theo id
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return toCategoryResponse(category);
    }

    // Mapping Response
    public CategoryResponse toCategoryResponse(Category category) {
        String catImg = category.getImageUrl();
        // Nếu không có ảnh đại diện riêng, thử lấy từ sản phẩm đầu tiên
        if (catImg == null || catImg.isBlank()) {
            if (category.getProducts() != null && !category.getProducts().isEmpty()) {
                var firstProd = category.getProducts().get(0);
                if (firstProd.getProductImages() != null && !firstProd.getProductImages().isEmpty()) {
                    catImg = firstProd.getProductImages().get(0).getImageUrl();
                }
            }
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .productCount(category.getProductCount() != null ? category.getProductCount() : 0)
                .imageUrl(catImg)
                .active(category.isActive())
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
