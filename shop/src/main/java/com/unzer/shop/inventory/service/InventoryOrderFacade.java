package com.unzer.shop.inventory.service;

import com.unzer.shop.inventory.model.Reservation;
import com.unzer.shop.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Extension methods on InventoryService for order-level operations.
 * Separated to keep InventoryService focused on single-variant operations.
 */
@Service
@RequiredArgsConstructor
public class InventoryOrderFacade {

    private final InventoryService inventoryService;
    private final ReservationRepository reservationRepository;

    @Transactional
    public void confirmReservationsByOrder(UUID orderId) {
        List<Reservation> reservations = reservationRepository.findByOrderId(orderId);
        reservations.forEach(r -> inventoryService.confirm(r.getId()));
    }

    @Transactional
    public void releaseReservationsByOrder(UUID orderId) {
        List<Reservation> reservations = reservationRepository.findByOrderId(orderId);
        reservations.forEach(r -> inventoryService.release(r.getId()));
    }
}
