package com.gomsu.contentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Review extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private String productName;
    private Long userId;
    private String username;
    private Integer rating;
    private String title; // Tiêu đề đánh giá

    @Column(columnDefinition = "TEXT")
    private String comment;
    private String imageReview;
    private Long orderId;

    @Builder.Default
    private boolean isApproved = false; // Mặc định chờ duyệt (trừ khi mua thật)

    @Column(name = "is_deleted")
    @Builder.Default
    private boolean isDeleted = false; // Xóa mềm

    private String adminReply;
    private LocalDateTime repliedAt;
}