package com.unzer.shop.payment.controller;

import com.unzer.shop.cart.model.Cart;
import com.unzer.shop.cart.service.CartService;
import com.unzer.shop.common.UnzerProperties;
import com.unzer.shop.inventory.service.InventoryService;
import com.unzer.shop.order.model.Order;
import com.unzer.shop.order.service.OrderService;
import com.unzer.shop.payment.model.Payment;
import com.unzer.shop.payment.model.PaymentMethod;
import com.unzer.shop.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Checkout flow:
 * POST /api/checkout/initiate  — create order, reserve stock
 * POST /api/checkout/pay       — initiate Unzer payment, get redirectUrl
 * GET  /api/checkout/return    — returnUrl callback from Unzer (after redirect)
 * GET  /api/checkout/status    — poll order/payment status
 */
@Slf4j
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final UnzerProperties unzerProperties;

    /** Step 1: convert cart to order and reserve stock. */
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiate(
            @RequestHeader("X-Session-Token") String sessionToken,
            @Valid @RequestBody InitiateRequest req) {

        Cart cart = cartService.getOrCreate(sessionToken);
        if (cart.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cart is empty"));
        }

        // Reserve stock for every line item, collecting reservation IDs
        List<UUID> reservationIds = cart.getItems().stream()
                .map(item -> inventoryService.reserve(
                        item.getVariantId(), item.getQuantity(), Duration.ofMinutes(15)))
                .toList();

        OrderService.ShippingAddress address = new OrderService.ShippingAddress(
                req.getStreet(), req.getCity(), req.getCountry(), req.getZip());

        Order order = orderService.createOrder(cart, address, null);

        // Link each reservation to the order so confirm/release can find them later
        reservationIds.forEach(rId ->
                inventoryService.linkReservationToOrder(rId, order.getId()));

        log.info("Checkout initiated: orderId={}, total={}", order.getId(), order.getTotalAmount());

        return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "total", order.getTotalAmount(),
                "currency", order.getCurrency(),
                "publicKey", unzerProperties.getPublicKey()
        ));
    }

    /** Step 2: initiate payment — returns redirectUrl for redirect-based methods. */
    @PostMapping("/pay")
    public ResponseEntity<Map<String, Object>> pay(@Valid @RequestBody PayRequest req) {
        Payment payment = paymentService.initiate(req.getOrderId(), req.getMethod(), req.getTypeId());

        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", payment.getId());
        response.put("status", payment.getStatus());
        if (payment.getRedirectUrl() != null) {
            response.put("redirectUrl", payment.getRedirectUrl());
            response.put("action", "REDIRECT");
        } else {
            response.put("action", "NONE");
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Step 3: returnUrl callback — customer arrives here after Unzer redirect.
     * We do NOT confirm payment here; we wait for the webhook.
     * This prevents the race where the redirect arrives before the webhook.
     */
    @GetMapping("/return")
    public ResponseEntity<Map<String, String>> returnCallback(@RequestParam UUID orderId) {
        Order order = orderService.get(orderId);
        log.info("Return callback for orderId={}, currentStatus={}", orderId, order.getStatus());
        return ResponseEntity.ok(Map.of(
                "orderId", orderId.toString(),
                "status", order.getStatus().name(),
                "message", "Payment confirmation in progress. Check /api/checkout/status?orderId=" + orderId
        ));
    }

    /** Poll endpoint — browser polls here after redirect to get final status. */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status(@RequestParam UUID orderId) {
        Order order = orderService.get(orderId);
        return ResponseEntity.ok(Map.of(
                "orderId", orderId.toString(),
                "status", order.getStatus().name()
        ));
    }

    @Data
    public static class InitiateRequest {
        @NotBlank private String street;
        @NotBlank private String city;
        @NotBlank private String country;
        @NotBlank private String zip;
    }

    @Data
    public static class PayRequest {
        @NotNull  private UUID orderId;
        @NotNull  private PaymentMethod method;
        private String typeId; // required for CARD; null for WERO / OPEN_BANKING
    }
}
