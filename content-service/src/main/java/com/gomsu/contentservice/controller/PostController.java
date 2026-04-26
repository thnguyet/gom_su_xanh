package com.gomsu.contentservice.controller;

import com.gomsu.contentservice.dto.request.PostRequest;
import com.gomsu.contentservice.dto.response.PostResponse;
import com.gomsu.contentservice.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}
