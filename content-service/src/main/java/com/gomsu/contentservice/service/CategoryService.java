package com.gomsu.contentservice.service;

import com.gomsu.contentservice.dto.request.CategoryRequest;
import com.gomsu.contentservice.dto.request.CategoryUpdateRequest;
import com.gomsu.contentservice.dto.response.CategoryResponse;
import com.gomsu.contentservice.entity.PostCategory;
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
        return toResponse(category);
    }

    public CategoryResponse getById(Long id) {
        PostCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
        return toResponse(category);
    }

    // 3. Thêm mới
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên danh mục đã tồn tại!");
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục ID: " + id));

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new RuntimeException("Tên danh mục mới đã tồn tại!");
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục để xóa!"));

        // 2. Kiểm tra điều kiện (Tùy chọn)
        // Nếu Nguyệt muốn xóa mềm thì thường không cần chặn gắt gao như xóa cứng,
        // nhưng vẫn nên check nếu có bài viết đang hoạt động.
        if (category.getPosts() != null && !category.getPosts().isEmpty()) {
            // Nếu xóa mềm, Nguyệt có thể đổi thông báo thành: "Hãy chuyển bài viết sang danh mục khác trước khi ẩn danh mục này!"
            throw new RuntimeException("Không thể ẩn danh mục đang chứa bài viết!");
        }

        // 3. Thực hiện XÓA MỀM (Soft Delete)
        category.setActive(false); // Chuyển trạng thái về false thay vì gọi repository.delete()

        // 4. Lưu lại thay đổi
        categoryRepository.save(category);

        log.info(">>> Đã xóa mềm (ẩn) danh mục ID: {} thành công.", id);
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