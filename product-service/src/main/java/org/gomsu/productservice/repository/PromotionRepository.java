package org.gomsu.productservice.repository;

import org.gomsu.productservice.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion,Long> {
    Page<Promotion> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
