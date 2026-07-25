package com.unzer.shop.cart.controller;

import com.unzer.shop.cart.model.Cart;
import com.unzer.shop.cart.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<Cart> getCart(@RequestHeader("X-Session-Token") String token) {
        return ResponseEntity.ok(cartService.getOrCreate(token));
    }

    @PostMapping("/items")
    public ResponseEntity<Cart> addItem(@RequestHeader("X-Session-Token") String token,
                                        @Valid @RequestBody AddItemRequest req) {
        return ResponseEntity.ok(cartService.addItem(
                token, req.getVariantId(), req.getSku(), req.getName(),
                req.getQuantity(), req.getUnitPrice(), req.getCurrency()));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<Cart> removeItem(@RequestHeader("X-Session-Token") String token,
                                           @PathVariable UUID variantId) {
        return ResponseEntity.ok(cartService.removeItem(token, variantId));
    }

    @Data
    public static class AddItemRequest {
        @NotNull  private UUID variantId;
        @NotBlank private String sku;
        @NotBlank private String name;
        @Min(1)   private int quantity;
        @NotNull  private BigDecimal unitPrice;
        @NotBlank private String currency;
    }
}
