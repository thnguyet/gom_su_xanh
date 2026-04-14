package org.gomsu.orderservice.repository;

import org.gomsu.orderservice.entity.Order;
import org.gomsu.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Tìm đơn hàng theo khách hàng + Trạng thái (nếu có) + Phân trang & Sắp xếp
    Page<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status, Pageable pageable);

    // Tìm tất cả đơn hàng của khách hàng + Phân trang & Sắp xếp
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
}
