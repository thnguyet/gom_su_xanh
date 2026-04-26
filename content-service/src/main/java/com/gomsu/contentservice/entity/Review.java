package com.gomsu.contentservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId; // ID sản phẩm từ Product Service

    @Column(nullable = false)
    private Long userId; // ID người dùng từ Identity Service

    private String username; // Lưu tên để hiển thị nhanh cho FE

    @Column(nullable = false)
    private int rating; // 1 -> 5 sao

    @Column(columnDefinition = "TEXT")
    private String comment;

    private String imageReview; // Khách có thể đính kèm ảnh thật

    private boolean isApproved = true; // Admin có thể ẩn đánh giá xấu nếu cần
}
