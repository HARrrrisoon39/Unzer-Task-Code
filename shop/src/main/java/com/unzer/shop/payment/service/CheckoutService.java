package com.unzer.shop.payment.service;

import com.unzer.shop.cart.service.CartService;
import com.unzer.shop.inventory.service.InventoryService;
import com.unzer.shop.order.model.Order;
import com.unzer.shop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartService cartService;
    private final OrderService orderService;
    private final InventoryService inventoryService;

    @Transactional
    public Order initiate(String sessionToken, OrderService.ShippingAddress address) {
        var cart = cartService.getOrCreate(sessionToken);
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        var reservationIds = cart.getItems().stream()
                .map(item -> inventoryService.reserve(
                        item.getVariantId(), item.getQuantity(), Duration.ofMinutes(15)))
                .toList();

        Order order = orderService.createOrder(cart, address, null);
        reservationIds.forEach(rId -> inventoryService.linkReservationToOrder(rId, order.getId()));
        return order;
    }
}
