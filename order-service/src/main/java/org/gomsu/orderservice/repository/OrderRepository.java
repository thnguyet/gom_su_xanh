package org.gomsu.orderservice.repository;

import org.gomsu.orderservice.entity.Order;
import org.gomsu.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Tìm đơn hàng theo khách hàng + Trạng thái (nếu có) + Phân trang & Sắp xếp
    Page<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status, Pageable pageable);

    // Tìm tất cả đơn hàng của khách hàng + Phân trang & Sắp xếp
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
            "(:customerId IS NULL OR o.customerId = :customerId) AND " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:startDate IS NULL OR o.orderDate >= :startDate) AND " +
            "(:endDate IS NULL OR o.orderDate <= :endDate)")
    Page<Order> findAllOrdersForAdmin(
            @Param("customerId") Long customerId,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
