package com.unzer.shop.inventory.service;

import com.unzer.shop.inventory.model.Inventory;
import com.unzer.shop.inventory.model.Reservation;
import com.unzer.shop.inventory.repository.InventoryRepository;
import com.unzer.shop.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final int MAX_RETRY = 3;

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Reserve stock using optimistic locking.
     * Retries up to MAX_RETRY times on version conflict.
     * Throws InsufficientStockException if stock is unavailable.
     */
    @Transactional
    public UUID reserve(UUID variantId, int qty, Duration ttl) {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            Inventory inv = inventoryRepository.findByVariantId(variantId)
                    .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + variantId));

            if (inv.getAvailable() < qty) {
                throw new InsufficientStockException(variantId, qty, inv.getAvailable());
            }

            int updated = inventoryRepository.reserveOptimistic(variantId, qty, inv.getVersion());
            if (updated == 1) {
                Reservation reservation = Reservation.builder()
                        .variantId(variantId)
                        .quantity(qty)
                        .expiresAt(Instant.now().plus(ttl))
                        .status(Reservation.ReservationStatus.RESERVED)
                        .build();
                return reservationRepository.save(reservation).getId();
            }
            // Version conflict — another transaction updated concurrently; retry
            log.debug("Optimistic lock conflict reserving variant {}, attempt {}", variantId, attempt + 1);
        }
        throw new ConcurrentReservationException(variantId);
    }

    @Transactional
    public void linkReservationToOrder(UUID reservationId, UUID orderId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        r.setOrderId(orderId);
        reservationRepository.save(r);
    }

    private void confirm(UUID reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        if (r.getStatus() == Reservation.ReservationStatus.CONFIRMED) return;

        inventoryRepository.confirmReservation(r.getVariantId(), r.getQuantity());
        r.setStatus(Reservation.ReservationStatus.CONFIRMED);
        reservationRepository.save(r);
    }

    private void release(UUID reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        if (r.getStatus() == Reservation.ReservationStatus.RELEASED) return;

        inventoryRepository.releaseReservation(r.getVariantId(), r.getQuantity());
        r.setStatus(Reservation.ReservationStatus.RELEASED);
        reservationRepository.save(r);
        log.info("Released reservation {} for variant {}", reservationId, r.getVariantId());
    }

    /** Background job: release expired reservations every 60 seconds. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void releaseExpiredReservations() {
        List<Reservation> expired = reservationRepository.findExpired(Instant.now());
        for (Reservation r : expired) {
            try {
                release(r.getId());
                log.info("Auto-released expired reservation {}", r.getId());
            } catch (Exception e) {
                log.error("Failed to release expired reservation {}: {}", r.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void confirmReservationsByOrder(UUID orderId) {
        reservationRepository.findByOrderId(orderId).forEach(r -> confirm(r.getId()));
    }

    @Transactional
    public void releaseReservationsByOrder(UUID orderId) {
        reservationRepository.findByOrderId(orderId).forEach(r -> release(r.getId()));
    }
}
