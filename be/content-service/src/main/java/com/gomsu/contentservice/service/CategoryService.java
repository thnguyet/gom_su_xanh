package com.gomsu.contentservice.service;

import com.gomsu.contentservice.dto.request.CategoryRequest;
import com.gomsu.contentservice.dto.request.CategoryUpdateRequest;
import com.gomsu.contentservice.dto.response.CategoryResponse;
import com.gomsu.contentservice.entity.PostCategory;
import com.gomsu.contentservice.exception.AppException;
import com.gomsu.contentservice.exception.ErrorCode;
import com.gomsu.contentservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 1. Xem danh sách (Lấy toàn bộ)
    public Page<CategoryResponse> getCategories(
            String keyword, Boolean isAdmin, Boolean active,
            int page, int size, String sortBy, String sortDir) {

        Boolean adminFlag = (isAdmin != null && isAdmin);
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Truyền cả adminFlag và biến activeParam (bộ lọc chủ động) xuống
        return categoryRepository.findAllByFilter(keyword, adminFlag, active, pageable)
                .map(this::toResponse);
    }

    // 2. Xem chi tiết theo Slug (Cho khách)
    public CategoryResponse getBySlug(String slug) {
        PostCategory category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return toResponse(category);
    }

    public CategoryResponse getById(Long id) {
        PostCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return toResponse(category);
    }

    // 3. Thêm mới
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        String slug = toSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + (int) (Math.random() * 100);
        }

        PostCategory category = PostCategory.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .active(true)
                .build();

        return toResponse(categoryRepository.save(category));
    }

    // 4. Cập nhật
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        PostCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
            category.setName(request.getName());
            category.setSlug(toSlug(request.getName()));
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        // 1. Tìm danh mục
        PostCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // 2. Kiểm tra nếu có bài viết liên quan thì không cho xóa cứng
        if (category.getPosts() != null && !category.getPosts().isEmpty()) {
            throw new AppException(ErrorCode.CATEGORY_HAS_POSTS);
        }

        // 3. Thực hiện XÓA CỨNG (Hard Delete)
        categoryRepository.delete(category);

        log.info(">>> Đã xóa cứng danh mục ID: {} thành công.", id);
    }

    // --- Hàm bổ trợ ---

    private CategoryResponse toResponse(PostCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .active(category.getActive())
                .build();
    }

    public String toSlug(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("^-+|-+$", "");
    }
}