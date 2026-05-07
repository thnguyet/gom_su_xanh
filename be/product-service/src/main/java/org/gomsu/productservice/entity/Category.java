package org.gomsu.productservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.util.List;

@Entity
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
@Builder
public class Category extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug; // Thêm slug cho Category

    private String imageUrl;
    
    private boolean active = true;

    private boolean deleted = false;

    @OneToMany(mappedBy = "category")
    private List<Product> products;

    @Formula("(SELECT COUNT(*) FROM products p WHERE p.category_id = id)")
    private Integer productCount;
}
