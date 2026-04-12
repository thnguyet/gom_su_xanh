package org.gomsu.orderservice.repository;

import org.gomsu.orderservice.entity.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingMethodRepository extends JpaRepository<ShippingMethod,Long> {
}
