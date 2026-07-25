package com.unzer.shop.inventory.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @Column(name = "variant_id")
    private UUID variantId;

    @Column(nullable = false)
    private int available;

    @Column(nullable = false)
    private int reserved;

    // Optimistic lock — central to the oversell prevention mechanism
    @Version
    @Column(nullable = false)
    private int version;
}
