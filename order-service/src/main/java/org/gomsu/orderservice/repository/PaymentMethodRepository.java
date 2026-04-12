package org.gomsu.orderservice.repository;

import org.gomsu.orderservice.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod,Long> {
}
