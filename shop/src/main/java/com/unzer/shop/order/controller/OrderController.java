package com.unzer.shop.order.controller;

import com.unzer.shop.customer.repository.CustomerRepository;
import com.unzer.shop.order.model.Order;
import com.unzer.shop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;

    @GetMapping
    public ResponseEntity<List<Order>> myOrders(@AuthenticationPrincipal String email) {
        UUID customerId = customerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + email))
                .getId();
        return ResponseEntity.ok(orderService.getByCustomer(customerId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.get(orderId));
    }
}
