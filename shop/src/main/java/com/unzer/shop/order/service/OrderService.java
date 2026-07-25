package com.unzer.shop.order.service;

import com.unzer.shop.cart.model.Cart;
import com.unzer.shop.order.model.*;
import com.unzer.shop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(Cart cart, ShippingAddress address, UUID customerId) {
        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .customerId(customerId)
                .status(OrderStatus.CREATED)
                .totalAmount(total)
                .currency("EUR")
                .street(address.street())
                .city(address.city())
                .country(address.country())
                .zip(address.zip())
                .build();

        // Save first to get the generated ID, then add lines referencing that ID
        Order saved = orderRepository.save(order);

        List<OrderLine> lines = cart.getItems().stream()
                .map(i -> OrderLine.builder()
                        .orderId(saved.getId())
                        .variantId(i.getVariantId())
                        .sku(i.getSku())
                        .name(i.getName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .currency(i.getCurrency())
                        .build())
                .toList();

        saved.getLines().addAll(lines);
        Order result = orderRepository.save(saved);
        log.info("Order {} created, total={} EUR", result.getId(), total);
        return result;
    }

    @Transactional
    public Order transition(UUID orderId, OrderStatus next, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        log.info("Order {} transitioning {} -> {}", orderId, order.getStatus(), next);
        order.transition(next, reason);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order get(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getByCustomer(UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public record ShippingAddress(String street, String city, String country, String zip) {}
}
