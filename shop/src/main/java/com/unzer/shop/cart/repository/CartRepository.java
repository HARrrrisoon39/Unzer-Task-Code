package com.unzer.shop.cart.repository;

import com.unzer.shop.cart.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findBySessionToken(String sessionToken);
    Optional<Cart> findByCustomerId(UUID customerId);
}
