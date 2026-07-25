package com.unzer.shop.payment.repository;

import com.unzer.shop.payment.model.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
    List<PaymentEvent> findByProcessedFalse();
    boolean existsByUnzerPaymentIdAndEventType(String unzerPaymentId, String eventType);
}
