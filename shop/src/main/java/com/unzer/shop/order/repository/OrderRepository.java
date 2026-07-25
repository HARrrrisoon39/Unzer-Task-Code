package com.unzer.shop.order.repository;

import com.unzer.shop.order.model.Order;
import com.unzer.shop.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    List<Order> findByStatus(OrderStatus status);
    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);
}
