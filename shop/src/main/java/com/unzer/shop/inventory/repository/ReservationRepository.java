package com.unzer.shop.inventory.repository;

import com.unzer.shop.inventory.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query("SELECT r FROM Reservation r WHERE r.status = 'RESERVED' AND r.expiresAt < :now")
    List<Reservation> findExpired(@Param("now") Instant now);

    List<Reservation> findByOrderId(UUID orderId);
}
