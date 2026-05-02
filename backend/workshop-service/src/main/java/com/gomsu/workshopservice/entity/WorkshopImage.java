package com.gomsu.workshopservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workshop_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id")
    @ToString.Exclude // Tránh lỗi vòng lặp vô tận khi in log
    private Workshop workshop;
}