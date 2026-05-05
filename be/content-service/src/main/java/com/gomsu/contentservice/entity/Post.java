package com.gomsu.contentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String thumbnail; // Link ảnh đại diện bài viết

    @Column(unique = true)
    private String slug; // Ví dụ: huong-dan-lam-gom-thu-cong

    private String summary; // Mô tả ngắn

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private PostCategory category;

    private Long authorId; // ID Admin viết bài (từ Identity Service)

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> images = new ArrayList<>();

    private boolean published = false;
}