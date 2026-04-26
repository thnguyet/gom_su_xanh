package com.gomsu.contentservice.repository;

import com.gomsu.contentservice.entity.PostCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<PostCategory,Long> {
}
