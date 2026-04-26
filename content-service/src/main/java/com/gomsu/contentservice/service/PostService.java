package com.gomsu.contentservice.service;

import com.gomsu.contentservice.dto.request.PostRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
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
