package com.gomsu.workshopservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "workshops")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workshop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "slug", unique = true, nullable = false) // BẮT BUỘC THÊM
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    // --- THỜI GIAN TỔ CHỨC (FE dùng để hiện lịch) ---
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // --- THỜI GIAN ĐĂNG KÝ (Để logic kiểm tra thời gian) ---
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationEndDate;

    private Double price;
    private Integer maxParticipants; // Giới hạn số người để tính vé còn lại

    @Column(columnDefinition = "TEXT")
    private String content;

    private String location; // Địa điểm (Để FE hiện bản đồ hoặc địa chỉ)

    // Các thông tin thêm để FE làm trang chi tiết đẹp hơn
    private String targetAudience;
    private String tools;
    private String benefits;

    // Quan hệ với bảng ảnh (Để lấy ảnh đại diện cho FE)
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<WorkshopImage> images;

    // Số lượng người tham gia hiện tại
    @Builder.Default
    @Column(name = "current_participants", nullable = false)
    private Integer currentParticipants = 0; //n mặc định bằng 0

    private Boolean active = true;
}