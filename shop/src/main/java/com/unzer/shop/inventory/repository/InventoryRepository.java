package com.unzer.shop.inventory.repository;

import com.unzer.shop.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByVariantId(UUID variantId);

    /**
     * Atomic compare-and-swap: decrement available, increment reserved.
     * Returns 1 on success, 0 if version mismatch or insufficient stock.
     * This is the core oversell-prevention query.
     */
    @Modifying
    @Query(value = """
            UPDATE inventory
            SET available = available - :qty,
                reserved  = reserved  + :qty,
                version   = version   + 1
            WHERE variant_id      = :variantId
              AND version         = :expectedVersion
              AND available       >= :qty
            """, nativeQuery = true)
    int reserveOptimistic(@Param("variantId") UUID variantId,
                          @Param("qty") int qty,
                          @Param("expectedVersion") int expectedVersion);

    @Modifying
    @Query(value = """
            UPDATE inventory
            SET reserved  = reserved  - :qty,
                available = available + :qty,
                version   = version   + 1
            WHERE variant_id = :variantId
              AND reserved   >= :qty
            """, nativeQuery = true)
    int releaseReservation(@Param("variantId") UUID variantId, @Param("qty") int qty);

    @Modifying
    @Query(value = """
            UPDATE inventory
            SET reserved = reserved - :qty,
                version  = version  + 1
            WHERE variant_id = :variantId
              AND reserved   >= :qty
            """, nativeQuery = true)
    int confirmReservation(@Param("variantId") UUID variantId, @Param("qty") int qty);
}
