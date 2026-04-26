package com.gomsu.contentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "post_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true) // Nên để unique để tránh trùng URL
    private String slug;

    private String description;

    @OneToMany(mappedBy = "category")
    private List<Post> posts;
}