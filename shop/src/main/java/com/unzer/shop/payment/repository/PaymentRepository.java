package com.unzer.shop.payment.repository;

import com.unzer.shop.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByUnzerPaymentId(String unzerPaymentId);
}
