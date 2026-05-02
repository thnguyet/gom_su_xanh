package com.gomsu.contentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "post_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PostCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false) // Nên để unique để tránh trùng URL
    private String slug;

    private String description;

    @OneToMany(mappedBy = "category")
    private List<Post> posts;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private Boolean active = true;
}