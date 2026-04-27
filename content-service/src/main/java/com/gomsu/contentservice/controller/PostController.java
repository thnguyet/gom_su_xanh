package com.gomsu.contentservice.controller;

import com.gomsu.contentservice.dto.request.PostRequest;
import com.gomsu.contentservice.dto.request.PostUpdateRequest;
import com.gomsu.contentservice.dto.response.PostResponse;
import com.gomsu.contentservice.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("*")
public class PostController {
    private final PostService postService;

    // 1. Tạo bài viết mới
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> createPost(
            @Valid @ModelAttribute PostRequest postRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        // Lấy claim userId và ép kiểu an toàn
        Object userIdClaim = jwt.getClaim("userId");
        Long adminId = (userIdClaim instanceof Number) ? ((Number) userIdClaim).longValue() : null;

        if (adminId == null) {
            log.error("Không tìm thấy userId trong Token! Claims hiện có: {}", jwt.getClaims());
            throw new RuntimeException("Xác thực người dùng thất bại");
        }

        PostResponse response = postService.createPost(postRequest, adminId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Cho khách xem bài viết (Dùng Slug)
    @GetMapping("/detail/{slug}")
    public ResponseEntity<PostResponse> getPostBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(postService.getPostBySlug(slug));
    }

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category, // Nhận String để User dùng slug "am-chen", Admin dùng ID "1"
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Authentication authentication) { // Tự động lấy thông tin user từ JWT

        // Kiểm tra quyền Admin (Nếu không có token hoặc không phải Admin -> false)
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(postService.getPosts(
                keyword, category, fromDate, toDate,
                isAdmin, page, size, sortBy, sortDir));
    }

    // Cho Admin sửa bài viết (Vẫn dùng ID cho chắc chắn)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @RequestPart("request") PostUpdateRequest request,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages) {
        return ResponseEntity.ok(postService.updatePost(id, request, newImages));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok("Đã xóa bài viết và toàn bộ hình ảnh liên quan thành công!");
    }
}
