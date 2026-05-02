package com.gomsu.contentservice.service;

import com.gomsu.contentservice.dto.request.PostRequest;
import com.gomsu.contentservice.dto.request.PostUpdateRequest;
import com.gomsu.contentservice.dto.response.CategoryResponse;
import com.gomsu.contentservice.dto.response.PostResponse;
import com.gomsu.contentservice.entity.Post;
import com.gomsu.contentservice.entity.PostCategory;
import com.gomsu.contentservice.entity.PostImage;
import com.gomsu.contentservice.repository.CategoryRepository;
import com.gomsu.contentservice.repository.PostImageRepository;
import com.gomsu.contentservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final PostImageRepository postImageRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public PostResponse createPost(PostRequest request, Long adminId) {
        // 1. Tìm Category
        PostCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại với ID: " + request.getCategoryId()));

        // --- BỔ SUNG: Chặn tạo bài viết nếu Category đã bị ẩn ---
        if (Boolean.FALSE.equals(category.getActive())) {
            throw new RuntimeException("Không thể tạo bài viết trong danh mục đã bị ẩn hoặc ngừng hoạt động!");
        }

        // 2. Tạo Slug & Post object
        String slug = toSlug(request.getTitle());
        if (postRepository.existsBySlug(slug)) {
            slug = slug + "-" + (int) (Math.random() * 1000);
        }

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .summary(request.getSummary() != null ? request.getSummary() : createSummary(request.getContent()))
                .slug(slug)
                .authorId(adminId)
                .published(request.isPublished())
                .category(category)
                .images(new ArrayList<>())
                .build();

        // 3. Xử lý Upload ảnh
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (int i = 0; i < request.getImages().size(); i++) {
                try {
                    MultipartFile file = request.getImages().get(i);
                    String url = cloudinaryService.uploadImage(file);

                    // Nếu là cái ảnh đầu tiên (index = 0), gán nó làm thumbnail luôn
                    if (i == 0) {
                        post.setThumbnail(url);
                    }

                    PostImage postImage = PostImage.builder()
                            .imageUrl(url)
                            .post(post)
                            .build();
                    post.getImages().add(postImage);
                } catch (IOException e) {
                    log.error("Lỗi upload ảnh: {}", e.getMessage());
                    throw new RuntimeException("Lỗi xử lý hình ảnh");
                }
            }
        }

        return toPostResponse(postRepository.save(post));
    }

    // 1. Lấy chi tiết bài viết
    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết " + slug));
        return toPostResponse(post);
    }

    // Lay danh sach bai viet
    public Page<PostResponse> getPosts(
            String keyword, String category, // Đổi từ Long sang String
            LocalDateTime fromDate, LocalDateTime toDate,
            boolean isAdmin, int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Truyền category (dạng String) xuống Repository
        Page<Post> posts = postRepository.searchPosts(
                keyword, category, fromDate, toDate, isAdmin, pageable);

        return posts.map(this::toPostResponse);
    }

    @Transactional
    public PostResponse updatePost(Long id, PostUpdateRequest request, List<MultipartFile> newImages) {
        // 1. Kiểm tra bài viết có tồn tại không
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết ID: " + id));

        // 2. Cập nhật các trường thông tin cơ bản (Chỉ cập nhật nếu không null/rỗng)
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            post.setTitle(request.getTitle());
            post.setSlug(toSlug(request.getTitle())); // Cập nhật slug tương ứng với title mới
        }

        if (request.getContent() != null && !request.getContent().isBlank()) {
            post.setContent(request.getContent());
        }

        if (request.getSummary() != null) {
            post.setSummary(request.getSummary());
        }

        // Lưu ý: Đổi kiểu trong DTO sang Boolean (Object) để check null được
        if (request.getPublished() != null) {
            post.setPublished(request.getPublished());
        }

        // 3. Cập nhật Category nếu có thay đổi
        if (request.getCategoryId() != null) {
            PostCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại bài viết!"));
            post.setCategory(category);
        }

        // 4. XỬ LÝ XÓA ẢNH CŨ
        if (request.getDeletedImageIds() != null && !request.getDeletedImageIds().isEmpty()) {
            post.getImages().removeIf(image -> {
                if (request.getDeletedImageIds().contains(image.getId())) {
                    try {
                        cloudinaryService.deleteImage(image.getImageUrl());
                        // Nếu xóa trúng thumbnail hiện tại thì reset để lát nữa gán lại
                        if (image.getImageUrl().equals(post.getThumbnail())) {
                            post.setThumbnail(null);
                        }
                        return true;
                    } catch (IOException e) {
                        log.error("Lỗi xóa ảnh cũ trên Cloudinary: {}", e.getMessage());
                    }
                }
                return false;
            });
        }

        // 5. XỬ LÝ THÊM ẢNH MỚI
        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                try {
                    String imageUrl = cloudinaryService.uploadImage(file);
                    PostImage postImage = PostImage.builder()
                            .imageUrl(imageUrl)
                            .post(post)
                            .build();
                    // Lưu vào danh sách ảnh của bài viết
                    post.getImages().add(postImage);
                } catch (IOException e) {
                    log.error("Lỗi upload ảnh lên Cloudinary: {}", e.getMessage());
                    throw new RuntimeException("Lỗi khi upload ảnh mới");
                }
            }
        }

        // 6. KIỂM TRA VÀ CẬP NHẬT LẠI THUMBNAIL
        // Nếu thumbnail bị xóa hoặc chưa có mà list ảnh lại có ảnh -> lấy ảnh đầu làm đại diện
        if ((post.getThumbnail() == null || post.getThumbnail().isBlank()) && !post.getImages().isEmpty()) {
            post.setThumbnail(post.getImages().get(0).getImageUrl());
        }

        // Lưu và trả về
        return toPostResponse(postRepository.save(post));
    }

    @Transactional
    public void deletePost(Long id) {
        // 1. Tìm bài viết
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết cần xóa với ID: " + id));

        // 2. XỬ LÝ XÓA ẢNH CHI TIẾT TRÊN CLOUDINARY
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            for (PostImage image : post.getImages()) {
                try {
                    cloudinaryService.deleteImage(image.getImageUrl());
                } catch (IOException e) {
                    log.error("Cảnh báo: Không thể xóa ảnh chi tiết tại URL: {}", image.getImageUrl());
                }
            }
        }

        // 3. XỬ LÝ XÓA THUMBNAIL (Nếu thumbnail không nằm trong danh sách images trên)
        if (post.getThumbnail() != null) {
            try {
                // Kiểm tra xem thumbnail có trùng với ảnh chi tiết nào đã xóa chưa để tránh xóa 2 lần
                // Nhưng CloudinaryService của Nguyệt đã có try-catch nên gọi thẳng cũng không sao
                cloudinaryService.deleteImage(post.getThumbnail());
            } catch (IOException e) {
                log.error("Cảnh báo: Không thể xóa ảnh thumbnail tại URL: {}", post.getThumbnail());
            }
        }

        // 4. XÓA TRONG DATABASE
        postRepository.delete(post);
        log.info("Đã xóa thành công bài viết ID: {} và các dữ liệu liên quan.", id);
    }

    // Hàm tự động cắt nội dung làm tóm tắt nếu admin không nhập
    private String createSummary(String content) {
        if (content.length() <= 150) return content;
        return content.substring(0, 147) + "...";
    }

    public PostResponse toPostResponse(Post post) {
        CategoryResponse categoryResponse = null;

        if (post.getCategory() != null) {
            categoryResponse = CategoryResponse.builder()
                    .id(post.getCategory().getId())
                    .name(post.getCategory().getName())
                    .slug(post.getCategory().getSlug())
                    .description(post.getCategory().getDescription())
                    .createdAt(post.getCategory().getCreatedAt())
                    .active(post.getCategory().getActive())
                    .build();
        }

        List<String> imageUrls = post.getImages() != null ?
                post.getImages().stream()
                        .map(PostImage::getImageUrl)
                        .toList() : new ArrayList<>();

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .content(post.getContent())
                .summary(post.getSummary())
                .thumbnail(post.getThumbnail() != null ? post.getThumbnail() :
                        (!imageUrls.isEmpty() ? imageUrls.get(0) : null))
                .images(imageUrls) // Gán danh sách ảnh vào đây
                .createdAt(post.getCreatedAt())
                .category(categoryResponse)
                .authorId(post.getAuthorId())
                .published(post.isPublished())
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
