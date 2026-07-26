package com.unzer.shop.cart.service;

import com.unzer.shop.cart.model.Cart;
import com.unzer.shop.cart.model.CartItem;
import com.unzer.shop.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    @Transactional
    public Cart getOrCreate(String sessionToken) {
        return cartRepository.findBySessionToken(sessionToken)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().sessionToken(sessionToken).build()));
    }

    @Transactional
    public Cart addItem(String sessionToken, UUID variantId, String sku, String name,
                        int quantity, BigDecimal unitPrice, String currency) {
        Cart cart = getOrCreate(sessionToken);
        cart.getItems().stream()
                .filter(i -> i.getVariantId().equals(variantId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + quantity),
                        () -> cart.addItem(CartItem.builder()
                                .variantId(variantId)
                                .sku(sku)
                                .name(name)
                                .quantity(quantity)
                                .unitPrice(unitPrice)
                                .currency(currency)
                                .build()));
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(String sessionToken, UUID variantId) {
        Cart cart = getOrCreate(sessionToken);
        cart.getItems().removeIf(i -> i.getVariantId().equals(variantId));
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public Cart get(UUID cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));
    }
}
