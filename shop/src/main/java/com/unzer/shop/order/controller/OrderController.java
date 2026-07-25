package com.unzer.shop.order.controller;

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

    @GetMapping
    public ResponseEntity<List<Order>> myOrders(@AuthenticationPrincipal String email) {
        // In a real app we'd resolve customerId from email; simplified here
        return ResponseEntity.ok(orderService.getByCustomer(UUID.randomUUID()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.get(orderId));
    }
}
