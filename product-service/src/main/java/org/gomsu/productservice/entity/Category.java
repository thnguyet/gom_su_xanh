package org.gomsu.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    private boolean deleted = false;

    @OneToMany(mappedBy = "category")
    private List<Product> products;

    @Formula("(SELECT COUNT(*) FROM products p WHERE p.category_id = id)")
    private Integer productCount;
}
