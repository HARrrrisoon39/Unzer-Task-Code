package com.unzer.shop.payment.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "unzer_payment_id", nullable = false)
    private String unzerPaymentId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "retrieve_url")
    private String retrieveUrl;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(nullable = false)
    private boolean processed;

    @PrePersist
    void prePersist() { if (receivedAt == null) receivedAt = Instant.now(); }
}
