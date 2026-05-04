package org.gomsu.orderservice.repository;

import org.gomsu.orderservice.entity.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod,Long> {
    @Query("SELECT s FROM PaymentMethod s WHERE " +
            "(:onlyActive IS NULL OR s.active = :onlyActive) AND " +
            "(:keyword IS NULL OR :keyword = '' OR CONCAT(s.id, '') LIKE CONCAT('%', :keyword, '%') OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR s.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR s.createdAt <= :toDate)")
    Page<PaymentMethod> searchMethods(
            @Param("onlyActive") Boolean onlyActive,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable); // Pageable xử lý luôn vụ Sort (tên, phí...) và Phân trang
}
